package cn.itcast.userprofile.platform.controller;


//import cn.itcast.up.common.HdfsTools;

import cn.itcast.up.common.HDFSUtils;
import cn.itcast.userprofile.platform.bean.Codes;
import cn.itcast.userprofile.platform.bean.HttpResult;
import cn.itcast.userprofile.platform.bean.dto.ModelDto;
import cn.itcast.userprofile.platform.bean.dto.TagDto;
import cn.itcast.userprofile.platform.bean.dto.TagModelDto;
import cn.itcast.userprofile.platform.service.TagService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
public class TagAndModelController {
    @Autowired
    private TagService tagService;

    /**
     * 123级标签添加
     * @param tags
     */
    @PutMapping("tags/relation")
    public void addTag(@RequestBody List<TagDto> tags){
        System.out.println(tags);
        tagService.saveTags(tags);
    }

    /**
     * 123级标签显示
     * @param pid
     * @param level
     * @return
     */
    @GetMapping("tags")
    //@RequestMapping(method = {RequestMethod.GET},path = "tags")
    public HttpResult<List<TagDto>> findTagByPidOrLevel(@RequestParam(required = false) Long pid, @RequestParam(required = false) Integer level){
        List<TagDto> list = null;
        //如果传过来的是父ID,那么就用父ID查询
        if (pid != null) {
            list = tagService.findByPid(pid);
        }
        //如果传过来的是等级,就按照等级进行查询
        if (level != null) {
            list = tagService.findByLevel(level);
        }
        //返回结果
        return new HttpResult<List<TagDto>>(Codes.SUCCESS, "查询成功", list);
    }


    /**
     * 4级标签新增
     * 一个4级标签对应1个模型
     * 标签:标签的名称,标签的规则,标签等级
     * 模型:有Jar包的路径,Jar包的执行计划.Jar包执行的主类,Jar包执行的时候额外的参数
     * @param tagModelDto
     * @return
     */
    @PutMapping("tags/model")
    public HttpResult putTagAndModel(@RequestBody TagModelDto tagModelDto){
        System.out.println(tagModelDto);
        tagService.addTagModel(tagModelDto.getTag(), tagModelDto.getModel());
        return new HttpResult(Codes.SUCCESS, "成功", null);
    }

    /**
     * 4级标签查询
     * @param pid
     * @return
     */
    @GetMapping("tags/model")
    public HttpResult getModel(Long pid){
        List<TagModelDto> dto = tagService.findModelByPid(pid);
        return new HttpResult(Codes.SUCCESS, "查询成功", dto);
    }

    /**
     * 5级标签新增
     * @param tagDto
     * @return
     */
    @PutMapping("tags/data")
    public HttpResult putData(@RequestBody TagDto tagDto){
        tagService.addDataTag(tagDto);
        return new HttpResult(Codes.SUCCESS, "添加成功", null);
    }

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/tags/upload")
    public HttpResult<String> postTagsFile(@RequestParam("file") MultipartFile file) {
        String basePath = "hdfs://bd001:8020/temp/jars/";
        String fileName = UUID.randomUUID().toString() + ".jar";
        String path = basePath + fileName;//path="hdfs://bd001:8020/temp/jars//xxx.jar"
        try {
            InputStream inputStream = file.getInputStream();
            IOUtils.copy(inputStream, new FileOutputStream(new File("temp.jar")));
            HDFSUtils.getInstance().copyFromFile("temp.jar",path);
            System.out.println("===>jar包已上传到:" + path);
            return new HttpResult<>(Codes.SUCCESS, "", path);
        } catch (IOException e) {
            e.printStackTrace();
            return new HttpResult<>(Codes.ERROR, "文件上传失败", null);
        }
    }

    /**
     * 启动/停止模型
     * @param id
     * @param modelDto
     * @return
     */
    @PostMapping("tags/{id}/model")
    public HttpResult changeModelState(@PathVariable Long id, @RequestBody ModelDto modelDto){
        tagService.updateModelState(id, modelDto.getState());
        return new HttpResult(Codes.SUCCESS, "执行成功", null);
    }
}
