package com.leyou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.client.BrandClient;
import com.leyou.client.CategoryClient;
import com.leyou.client.GoodsClient;
import com.leyou.client.SpecificationClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.repository.GoodsRepository;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private GoodsRepository goodsRepository;

    // 根据spu,生成Goods，存入索引库
    public Goods buildGoods(Spu spu) {
        Long spuId = spu.getId();

        // 查询商品分类
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if (CollectionUtils.isEmpty(categories)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        List<String> categoryName = categories.stream().map(Category::getName).collect(Collectors.toList());
        // 查询商品品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        // 搜索字段
        String all = spu.getTitle() + StringUtils.join(categoryName," ") +brand.getName();

        // 查询sku
        List<Sku> skuList = goodsClient.querySkuBySpuId(spuId);
        if (CollectionUtils.isEmpty(skuList)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        List<Map<String,Object>> skus = new ArrayList<>();
        List<Long> priceList = new ArrayList<>();
        for (Sku sku : skuList) {
            Map<String,Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("price",sku.getPrice());
            map.put("title",sku.getTitle());
            map.put("images",StringUtils.substringBefore(sku.getImages(),","));
            skus.add(map);
            // sku的价格
            priceList.add(sku.getPrice());
        }

        // 商品规格参数
        List<SpecParam> specParams = specificationClient.queryParamByGid(null, spu.getCid3(), null, true);
        if (CollectionUtils.isEmpty(specParams)) {
            throw new LyException(ExceptionEnum.SPEC_PARAM_NOT_FOUND);
        }
        List<Long> specParamsIds = specParams.stream().map(SpecParam::getId).collect(Collectors.toList());

        // 查询商品详情
        SpuDetail spuDetail = goodsClient.querySpuDetailById(spuId);
        Map<Long, String> genericSpec = JsonUtils.parseMap(spuDetail.getGenericSpec(), Long.class, String.class);
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });

        // 规格参数，key是规格参数的名字，value是规格参数的值
        Map<String,Object> specs = new HashMap<>();
        for (SpecParam param : specParams) {
            String key = param.getName();
            Object value = "";
            if (param.getGeneric()) {
                value = genericSpec.get(param.getId());
                // 判断是否是数字
                if (param.getNumeric()) {
                    // 处理成段
                    value = chooseSegment(value.toString(),param);
                }
            } else {
                value = specialSpec.get(param.getId());
            }
            specs.put(key,value);
        }

        Goods goods = new Goods();
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spuId);
        goods.setSubTitle(spu.getSubTitle());
        goods.setAll(all); // 搜索字段，包含标题、分类、品牌、规格等
        goods.setPrice(priceList); // 所有sku的价格
        goods.setSpecs(specs); // 所有可搜索的规格参数
        goods.setSkus(JsonUtils.serialize(skus)); // 所有sku的json格式

        return goods;

    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }


    public PageResult<Goods> search(SearchRequest request) {
        String keyword = request.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(keyword)){
            return null;
        }

        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 1.对key进行全文检索
        queryBuilder.withQuery(QueryBuilders.matchQuery("all",keyword).operator(Operator.AND));
        // 2、通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        // 3、分页
        queryBuilder.withPageable(PageRequest.of(request.getPage(),request.getSize()));
        // 4、查询
        Page<Goods> goods = this.goodsRepository.search(queryBuilder.build());

        return new PageResult<Goods>(goods.getTotalElements(), (long) goods.getTotalPages(), goods.getContent());
    }
}
