package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.pojo.Stock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private StockMapper stockMapper;

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

    public void saveGoods(Spu spu) {
        //新增商品spu
        spu.setId(null);
        spu.setSaleable(true);
        spu.setValid(false);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        int count = spuMapper.insert(spu);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_INSERT_ERROR);
        }

        //新增detail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.insert(spuDetail);
        if (count != 1){
            throw new LyException(ExceptionEnum.GOODS_INSERT_ERROR);
        }

        //新增sku和stock
        saveSkuAndStock(spu);

    }

    private void saveSkuAndStock(Spu spu) {
        int count;//存放stock
        List<Stock> stockList = new ArrayList<>();
        //新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            if (!sku.getEnable()){
                continue;
            }
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());

            count = skuMapper.insert(sku);
            if (count != 1){
                throw new LyException(ExceptionEnum.GOODS_INSERT_ERROR);
            }

            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockList.add(stock);
        }

        //批量新增库存
        count = stockMapper.insertList(stockList);
        if (count != stockList.size()){
            throw new LyException(ExceptionEnum.GOODS_INSERT_ERROR);
        }
    }

    public SpuDetail querySpuDetailById(Long spuId) {
        return spuDetailMapper.selectByPrimaryKey(spuId);

    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        //查询sku
        Sku record = new Sku();
        record.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(record);

        //同时为页面显示库存
        for (Sku sku : skuList) {
            sku.setStock(stockMapper.selectByPrimaryKey(sku.getId()).getStock());
        }
        return skuList;
    }

    public void updateGoods(Spu spu) {
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        //查询sku
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)){
            List<Long> ids = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            //删除sku
            skuMapper.delete(sku);
            //删除stock
            stockMapper.deleteByIdList(ids);
        }

        //修改spu
        spu.setLastUpdateTime(new Date());
        spu.setCreateTime(null);
        spu.setValid(null);
        spu.setSaleable(null);
        spuMapper.updateByPrimaryKeySelective(spu);
        //修改detail
        spuDetailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());

        //新增sku和stock
        saveSkuAndStock(spu);


    }
}
