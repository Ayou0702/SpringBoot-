package com.enterprise.controller.data;

import com.enterprise.common.handler.Result;
import com.enterprise.entity.CurriculumData;
import com.enterprise.service.CurriculumDataService;
import com.enterprise.util.CurriculumDataUtil;
import com.enterprise.util.DateUtil;
import com.enterprise.util.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

/**
 * 负责线性课程表数据的Controller
 *
 * @author PrefersMin
 * @version 1.4
 */
@RestController
@RequiredArgsConstructor
public class CurriculumDataController {

    /**
     * 日期工具类
     */
    private final DateUtil dateUtil;

    /**
     * 线性课程表数据工具类
     */
    private final CurriculumDataUtil curriculumDataUtil;

    /**
     * 线性课程表数据接口
     */
    private final CurriculumDataService curriculumDataService;

    /**
     * 事务管理器
     */
    private final PlatformTransactionManager platformTransactionManager;

    /**
     * 获取所有线性课程表数据
     *
     * @author PrefersMin
     *
     * @return 返回获取到的线性课程表数据
     */
    @GetMapping("/queryAllCurriculumData")
    public Result queryAllCurriculumData() {

        List<CurriculumData> curriculumDataList = curriculumDataService.queryAllCurriculumData();

        if (curriculumDataList == null) {
            return Result.failed().message("课程推送队列数据加载失败");
        }

        return Result.success().message("课程推送队列数据加载成功").data("curriculumDataList", curriculumDataList);

    }

    /**
     * 重置线性课程表
     *
     * @author PrefersMin
     *
     * @return 返回重置结果
     */
    @GetMapping("/resetCurriculumData")
    public Result resetCurriculumData() {

        boolean resetResult = curriculumDataUtil.resetCurriculumData().getCode() == 200;
        if (!resetResult) {
            return Result.failed().message("重置失败").description("课程推送队列重置失败");
        }
        return Result.success().message("重置成功").description("课程推送队列重置成功");

    }

    /**
     * 查询今日以及之后的所有线性课程表数据
     *
     * @author PrefersMin
     *
     * @return 返回查询结果
     */
    @GetMapping("/getAllNowCurriculumData")
    public Result getAllNowCurriculumData() {

        int week = dateUtil.getW();
        int period = dateUtil.getPeriod();

        LogUtil.info("当前查询的是 " + period + "周 星期" + week + " 及以后的所有课程");

        List<CurriculumData> curriculumDataList = curriculumDataService.getAllNowCurriculumData(period, week);

        if (curriculumDataList == null) {
            return Result.failed().message("课程推送队列数据加载失败");
        }

        return Result.success().message("课程推送队列数据加载成功").data("curriculumDataList", curriculumDataList);
    }

    @PostMapping("/deleteCurriculumData")
    public Result deleteCurriculumData(@RequestBody List<Integer> curriculumIdList) {

        // 开始事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        // 记录操作结果
        List<String> record = new ArrayList<>();
        List<String> failedRecord = new ArrayList<>();

        for (int curriculumId : curriculumIdList) {
            if (isNull(curriculumDataService.queryCurriculumDataByCurriculumId(curriculumId))) {
                LogUtil.error("ID为" + curriculumId + "的课程推送队列数据删除失败,数据不存在");
                failedRecord.add("ID为" + curriculumId + "的课程推送队列数据删除失败,数据不存在，请刷新页面");
            }
        }

        if (!failedRecord.isEmpty()) {
            failedRecord.forEach(LogUtil::error);
            // 回滚事务
            platformTransactionManager.rollback(transactionStatus);
            return Result.failed().message("删除课程推送队列数据失败").description(failedRecord.toString());
        }

        for (Integer curriculumId : curriculumIdList) {
            boolean deleteResult = curriculumDataService.deleteCurriculumDataByCurriculumId(curriculumId);
            if (!deleteResult) {
                LogUtil.error("ID为" + curriculumId + "的课程推送队列数据删除失败");
                // 回滚事务
                platformTransactionManager.rollback(transactionStatus);
                return Result.failed().message("删除失败").description("序列ID为" + curriculumId + "的课程推送队列数据删除失败");
            }
            record.add("ID为" + curriculumId + "的课程推送队列数据删除成功");
        }

        record.forEach(LogUtil::info);
        LogUtil.info(curriculumIdList.size() + "条课程推送队列数据被删除");
        // 提交事务
        platformTransactionManager.commit(transactionStatus);
        return Result.success().message("删除成功").description(curriculumIdList.size() + "条课程推送队列数据被删除");

    }

    /**
     * 新增课程推送队列数据
     *
     * @author PrefersMin
     *
     * @param curriculumData 需要新增的课程推送队列数据
     * @return 返回新增结果
     */
    @PostMapping("/addCurriculumData")
    public Result addCurriculumData(@RequestBody CurriculumData curriculumData) {

        // 开始事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        String message;
        boolean duplicateResult = curriculumDataService.preciseQueryCurriculumDataByTime(curriculumData.getCurriculumPeriod(), curriculumData.getCurriculumWeek(), curriculumData.getCurriculumSection()) == null;

        if (!duplicateResult) {
            LogUtil.error("课程时间冲突： " + curriculumData.getCurriculumPeriod() + " 周、星期 " + curriculumData.getCurriculumWeek() + "、第 " + curriculumData.getCurriculumSection() + " 节");
            LogUtil.error("课程推送队列数据：" + curriculumData);
            // 回滚事务
            platformTransactionManager.rollback(transactionStatus);
            return Result.failed().message("新增失败").description("新增的课程推送队列数据与已有数据存在推送时间冲突");

        }

        boolean insertResult = curriculumDataService.addCurriculumData(curriculumData);

        if (insertResult) {
            message = "课程名称为" + curriculumData.getCourseName() + "的课程推送队列数据新增成功";
            LogUtil.info(message);
            // 提交事务
            platformTransactionManager.commit(transactionStatus);
            return Result.success().message("新增课程推送队列数据成功").description(message);
        }

        LogUtil.error("新增课程推送队列数据失败，课程推送队列数据：" + curriculumData);
        // 回滚事务
        platformTransactionManager.rollback(transactionStatus);
        return Result.success().message("新增课程推送队列数据失败").description("课程名称为" + curriculumData.getCourseName() + "的课程推送队列数据新增失败");

    }

    /**
     * 更新课程推送队列数据
     *
     * @author PrefersMin
     *
     * @param curriculumData 需要更新的课程推送队列数据
     * @return 返回更新结果
     */
    @PostMapping("/updateCurriculumData")
    public Result updateCurriculumData(@RequestBody CurriculumData curriculumData) {

        // 开始事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());

        String message;

        if (isNull(curriculumDataService.queryCurriculumDataByCurriculumId(curriculumData.getCurriculumId()))) {
            message = "序列ID为" + curriculumData.getCurriculumId() + "的课程推送队列数据更新失败,数据不存在";
            LogUtil.error(message);
            // 回滚事务
            platformTransactionManager.rollback(transactionStatus);
            return Result.failed().message("更新课程推送队列数据失败").description(message);
        }

        boolean updateResult = curriculumDataService.updateCurriculumData(curriculumData);

        if (updateResult) {
            message = "序列ID为 " + curriculumData.getCurriculumId() + " 的课程推送队列数据被修改";
            LogUtil.info(message);
            // 提交事务
            platformTransactionManager.commit(transactionStatus);
            return Result.success().message("修改课程推送队列数据成功").description(message);
        }

        LogUtil.error("序列ID为" + curriculumData.getCurriculumId() + "的课程推送队列数据修改失败");
        // 回滚事务
        platformTransactionManager.rollback(transactionStatus);
        return Result.failed().message("修改课程推送队列数据失败").description("序列ID为" + curriculumData.getCurriculumId() + "的课程推送队列数据修改失败");

    }

}
