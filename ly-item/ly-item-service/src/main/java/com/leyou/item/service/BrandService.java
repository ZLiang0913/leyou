package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

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

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    public void saveBrand(Brand brand, List<Long> cids) {
        //新增品牌
        int count = brandMapper.insert(brand);
        if (count!=1){
            throw new LyException(ExceptionEnum.BRAND_CREATE_ERROR);
        }

        for (Long cid : cids) {
            //新增品牌分类中间表
            count = brandMapper.insertCategoryBrand(cid, brand.getId());
            if (count!=1){
                throw new LyException(ExceptionEnum.BRAND_CREATE_ERROR);
            }
        }

    }

    /**
     * 根据品牌id获取品牌name
     * @param id
     * @return
     */
    public Brand queryById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand ==null ){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;

    }

    /**
     * 根据品牌分类中间表，通过分类id查找品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {

        List<Brand> brands = brandMapper.queryBrandByCategoryId(cid);
        if (CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
