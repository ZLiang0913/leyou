package com.leyou.item.web;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {
    @Autowired
    private SpecificationService specificationService;

    @GetMapping("groups/{cid}")
    public ResponseEntity<List<SpecGroup>> queryGroupByCid(@PathVariable("cid") Long cid){
        return ResponseEntity.ok(specificationService.queryGroupByCid(cid));
    }

    /**
     * 新增规格组
     * @param specGroup
     * @return
     */
    @PostMapping("group")
    public ResponseEntity<Void> insertGroup(@RequestBody SpecGroup specGroup){
        specificationService.saveGroup(specGroup);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 删除规格组
     * @param id
     * @return
     */
    @DeleteMapping("group/{id}")
    public ResponseEntity<Void> deleteGroupById(@PathVariable("id") Long id){
        specificationService.deleteGroupById(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 跟新规格组
     * @param specGroup
     * @return
     */
    @PutMapping("group")
    public ResponseEntity<Void> updateGroup(@RequestBody SpecGroup specGroup){
        specificationService.updateGroup(specGroup);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
