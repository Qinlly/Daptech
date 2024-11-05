package com.example.daptech.service.impl;

import com.example.daptech.entity.PhoneAppeal;
import com.example.daptech.entity.PhoneCn;
import com.example.daptech.entity.PhoneMark;
import com.example.daptech.mapper.PhoneAppealMapper;
import com.example.daptech.mapper.PhoneCnMapper;
import com.example.daptech.mapper.PhoneMarkMapper;
import com.example.daptech.response.Result;
import com.example.daptech.service.PhoneAppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class PhoneAppealServiceImpl implements PhoneAppealService {

    private final PhoneAppealMapper phoneAppealMapper;
    private final PhoneMarkMapper phoneMarkMapper;
    private final PhoneCnMapper phoneCnMapper;

    // 获取当前时间的 Instant 对象
    Instant now = Instant.now();

    // 获取 Unix 时间戳（秒）
    long unixTimestampSeconds = now.getEpochSecond();

    public boolean canAppealPhoneNumber(Long userId) { //判断是否可以提交申诉,可以为true,否则为false
        PhoneAppeal lastMark = phoneAppealMapper.findLastAppeal(userId);
        if (lastMark==null) {
            return  true;
        }else{
            Instant now = Instant.now();

            // 获取 Unix 时间戳（秒）
            long unixTimestampSeconds = now.getEpochSecond();

            long lastTime = lastMark.getCreateTime();

            return (unixTimestampSeconds - lastTime) >= 60*60*12; //间隔超过12小时才可以再次提交申诉
        }

    }
    @Override
    public Result submitAppeal(String phoneNumber, Long userId) {  //向phone_appeal表中插入一条记录,提交申诉加入审核队列
        if(canAppealPhoneNumber(userId)){
            PhoneAppeal appeal = new PhoneAppeal();
            appeal.setPhone(phoneNumber);
            appeal.setCreateTime(unixTimestampSeconds);
            appeal.setStatus(0);
            appeal.setUserId(userId);
            phoneAppealMapper.insertAppeal(appeal);
            return Result.success("提交申诉成功,请等待系统审核");
        }else{
            return Result.error("提交申诉过于频繁,请等待12小时后再试");
        }
    }

    @Override
    public void autoReviewAppeals() {  //自动审核号码申诉
        List<PhoneAppeal> pendingAppeals = phoneAppealMapper.findPendingAppeals(); //找到所有待审核的申诉
        for (PhoneAppeal appeal : pendingAppeals) { //对于每一条待审核的申诉,判断当天是否有五次以上标记
            if (isAppealApproved(appeal)) { //标记小于等于五次,则不会审核失败
                if(appeal.getCreateTime() < (unixTimestampSeconds - 7 * 24 * 60 * 60)) {//如果此时已经超过七天,则审核通过,否则等待下一次审核
                    approveAppeal(appeal);
                    phoneMarkMapper.deleteMark(appeal.getPhone(), appeal.getCreateTime());
                }
            } else { //标记大于五次,则审核失败
                rejectAppeal(appeal);
            }
        }
    }

    @Override
    public Result getAppeal(Long userId) {
        List<PhoneAppeal> appeals = phoneAppealMapper.findAppeal(userId);

        if(appeals.isEmpty()){
            return Result.error("无申诉信息");
        }
        return Result.success(appeals);
    }



    private boolean isAppealApproved(PhoneAppeal appeal) { //验证七天内是否有五次以上标记
        long sevenDaysAgo = now.getEpochSecond() - 7 * 24 * 60 * 60 ;

        long markCount = phoneMarkMapper.countByPhoneNumberAndCreatedAtAfter(appeal.getPhone(), sevenDaysAgo);
        return markCount <= 5;
    }

    private void approveAppeal(PhoneAppeal appeal) {
        appeal.setStatus(1); // 审核通过
        phoneAppealMapper.updateAppealStatus(appeal);
        phoneCnMapper.deletePhoneCn(appeal.getPhone());
    }

    private void rejectAppeal(PhoneAppeal appeal) {
        appeal.setStatus(2); // 审核不通过
        phoneAppealMapper.updateAppealStatus(appeal);

    }

}
