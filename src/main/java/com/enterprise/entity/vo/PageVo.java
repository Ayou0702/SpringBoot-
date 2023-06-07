package com.enterprise.entity.vo;

import com.enterprise.entity.CurriculumData;
import lombok.Data;

import java.util.List;

/**
 * 分页对象实体类
 *
 * @author PrefersMin
 * @version 1.0
 */
@Data
public class PageVo {

    private int pageIndex, pageSize, totalCount;
    private List<CurriculumData> curriculumDataList;

}
