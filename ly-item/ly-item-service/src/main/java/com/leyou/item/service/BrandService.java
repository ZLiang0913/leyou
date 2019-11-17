package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandByPageAndSort(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        //开始分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Brand.class);
        //搜索关键字
        if (StringUtils.isNotBlank(key)){
            example.createCriteria().orLike("name","%"+key+"%").orEqualTo("letter",key.toUpperCase());
        }
        //排序
        if (StringUtils.isNotBlank(sortBy)){
            String orderByClause = sortBy+ (desc ? " DESC":" ASC");
            example.setOrderByClause(orderByClause);
        }

        //查询
        Page<Brand> pageinfo = (Page<Brand>) this.brandMapper.selectByExample(example);

        return new PageResult<Brand>(pageinfo.getTotal(),pageinfo);
    }
}
