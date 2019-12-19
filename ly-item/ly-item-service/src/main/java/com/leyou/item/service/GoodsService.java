package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.Spu;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private BrandService brandService;
    @Autowired
    private CategoryService categoryService;

    public PageResult<Spu> querySpuBypage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page, rows);

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //过滤
        if (StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        if (saleable != null){
            criteria.andEqualTo("saleable",saleable);
        }

        //默认排序
        example.setOrderByClause("last_update_time DESC");

        //查询
        List<Spu> spuList = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spuList)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }

        //解析分类和品牌的名称
        loadBrandAndCategoryName(spuList);

        Page<Spu> pageInfo = (Page<Spu>) spuList;

        return new PageResult<Spu>(pageInfo.getTotal(), pageInfo);

    }

    private void loadBrandAndCategoryName(List<Spu> spuList) {
        for (Spu spu : spuList) {
            //品牌名称
            spu.setBname(brandService.queryById(spu.getBrandId()).getName());

            //分类名称
            List<String> categoryName = categoryService.queryListByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3())).stream().map(Category::getName).collect(Collectors.toList());
            String cname = StringUtils.join(categoryName, "/");
            spu.setCname(cname);
        }
    }
}
