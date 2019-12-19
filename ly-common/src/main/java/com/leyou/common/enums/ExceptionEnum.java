package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ExceptionEnum {
    PRICE_CAN_NOT_BE_NULL(400,"价格不能为空"),
    SPEC_GROUP_NOT_FOUND(404,"商品规格组不存在"),
    SPEC_PARAM_NOT_FOUND(404,"商品规格参数不存在"),
    CATEGORY_NOT_FOUND(404,"商品分类没查到"),
    GOODS_NOT_FOUND(404,"商品没查到"),
    BRAND_CREATE_ERROR(500,"新增品牌失败"),
    SPEC_CREATE_ERROR(500,"新增规格组失败"),
    SPEC_PARAM_CREATE_ERROR(500,"新增规格参数失败"),
    ;
    private int code;
    private String msg;
}
