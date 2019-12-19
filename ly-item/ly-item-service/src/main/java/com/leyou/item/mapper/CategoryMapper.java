package com.leyou.item.mapper;

import com.leyou.item.pojo.Category;
import org.apache.ibatis.annotations.Mapper;
import tk.mybatis.mapper.additional.idlist.IdListMapper;

@Mapper
public interface CategoryMapper extends tk.mybatis.mapper.common.Mapper<Category>, IdListMapper<Category,Long> {
}
