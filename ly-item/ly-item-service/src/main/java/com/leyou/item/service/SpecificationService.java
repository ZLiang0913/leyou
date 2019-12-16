package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.pojo.SpecGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;

@Service
public class SpecificationService {
    @Autowired
    private SpecGroupMapper specGroupMapper;

    public List<SpecGroup> queryGroupByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        List<SpecGroup> specGroupList = specGroupMapper.select(specGroup);

        if (CollectionUtils.isEmpty(specGroupList)){
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return specGroupList;
    }

    public void saveGroup(SpecGroup specGroup) {
        int insert = specGroupMapper.insert(specGroup);
        if (insert!=1){
            throw new LyException(ExceptionEnum.SPEC_REATE_ERROR);
        }
    }

    public void deleteGroupById(Long id) {
        specGroupMapper.deleteByPrimaryKey(id);
    }

    public void updateGroup(SpecGroup specGroup) {
        specGroupMapper.updateByPrimaryKey(specGroup);
    }
}
