package cn.itcast.userprofile.platform.service.impl;

import cn.itcast.userprofile.platform.bean.dto.ModelDto;
import cn.itcast.userprofile.platform.bean.dto.TagDto;
import cn.itcast.userprofile.platform.bean.dto.TagModelDto;
import cn.itcast.userprofile.platform.bean.po.ModelPo;
import cn.itcast.userprofile.platform.bean.po.TagPo;
import cn.itcast.userprofile.platform.repo.ModelRepository;
import cn.itcast.userprofile.platform.repo.TagRepository;
import cn.itcast.userprofile.platform.service.Engine;
import cn.itcast.userprofile.platform.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {
    @Autowired
    private TagRepository tagRepo;
    @Autowired
    private ModelRepository modelRepo;
    @Autowired
    private Engine engine;


    @Override
    public void saveTags(List<TagDto> tags) {
        //[TagDto(id=null, name=银行, rule=null, level=1, pid=-1),
        // TagDto(id=null, name=北京银行, rule=null, level=2, pid=null),
        // TagDto(id=null, name=人口属性, rule=null, level=3, pid=null)]
        //将tags保存到数据库
        //保存1级标签
        //得到1级标签的ID
        //将1级的ID给2级的父ID
        //保存2级标签
        //得到2级标签的ID
        //将2级的ID给3级的父ID

        //将dto=>po
        TagDto tag1 = tags.get(0);
        TagDto tag2 = tags.get(1);
        TagDto tag3 = tags.get(2);
        //转换
        TagPo tagPo1 = convert(tag1);
        TagPo tagPo2 = convert(tag2);
        TagPo tagPo3 = convert(tag3);

        //定义一个临时变量,来保存ID
        TagPo tmp = null;

        //存之前,先判断有没有
        TagPo  tagResult  = tagRepo.findByNameAndLevelAndPid(tagPo1.getName(), tagPo1.getLevel(),tagPo1.getPid());
        if (tagResult == null) {
            //第一次来,开始存储
            tmp = tagRepo.save(tagPo1);
        } else {
            //如果找到了,直接使用.
            tmp = tagResult;
        }

        //将临时变量中的ID取出来,作为2级的父级ID
        tagPo2.setPid(tmp.getId());
        TagPo tagResult2 = tagRepo.findByNameAndLevelAndPid(tagPo2.getName(), tagPo2.getLevel(),tagPo2.getPid());
        if (tagResult2 == null) {
            //第一次来,开始存储
            tmp = tagRepo.save(tagPo2);
        } else {
            //如果找到了,直接使用.
            tmp = tagResult2;
        }
        //将临时变量中的ID取出来,作为3级的父级ID
        tagPo3.setPid(tmp.getId());
        TagPo  tagResult3 = tagRepo.findByNameAndLevelAndPid(tagPo3.getName(), tagPo3.getLevel(),tagPo3.getPid());
        if (tagResult3 == null) {
            //第一次来,开始存储
            tmp = tagRepo.save(tagPo3);
        } else {
            //如果找到了,直接使用.
            tmp = tagResult3;
        }
    }

    @Override
    public List<TagDto> findByPid(Long pid) {
        List<TagPo> list = tagRepo.findByPid(pid);
        //将po集合转换为dto集合.
        /*for (TagPo tagPo : list) {
            TagDto dto = convert(tagPo);
        }*/
        return list.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<TagDto> findByLevel(Integer level) {
        List<TagPo> list = tagRepo.findByLevel(level);
        List<TagDto> listDto = list.stream().map(this::convert).collect(Collectors.toList());
        return listDto;
    }


    @Override
    public void addTagModel(TagDto tagDto, ModelDto modelDto) {
        //保存tag
        TagPo tagPo = tagRepo.save(convert(tagDto));
        //保存model
        modelRepo.save(convert(modelDto, tagPo.getId()));
    }

    @Override
    public List<TagModelDto> findModelByPid(Long pid) {
        List<TagPo> tagPos = tagRepo.findByPid(pid);
        return tagPos.stream().map((tagPo) -> {
            Long id = tagPo.getId();
            ModelPo modelPo = modelRepo.findByTagId(id);
            if (modelPo == null) {
                //找不到model,就只返回tag
                return new TagModelDto(convert(tagPo),null);
            }
            return new TagModelDto(convert(tagPo), convert(modelPo));
        }).collect(Collectors.toList());
    }

    @Override
    public void addDataTag(TagDto tagDto) {
        tagRepo.save(convert(tagDto));
    }


    @Override
    public void updateModelState(Long id, Integer state) {
        ModelPo modelPo = modelRepo.findByTagId(id);
        //如果传递过来的状态是3,那么就是启动,如果是4那么就是停止
        if (state == ModelPo.STATE_ENABLE) {
            //启动流程
            engine.startModel(convert(modelPo));
        }
        if (state == ModelPo.STATE_DISABLE) {
            //关闭流程
            engine.stopModel(convert(modelPo));
        }
        //更新状态信息
        modelPo.setState(state);
        modelRepo.save(modelPo);
    }



    private ModelDto convert(ModelPo modelPo) {
        ModelDto modelDto = new ModelDto();
        modelDto.setId(modelPo.getId());
        modelDto.setName(modelPo.getName());
        modelDto.setMainClass(modelPo.getMainClass());
        modelDto.setPath(modelPo.getPath());
        modelDto.setArgs(modelPo.getArgs());
        modelDto.setState(modelPo.getState());
        modelDto.setSchedule(modelDto.parseDate(modelPo.getSchedule()));
        return modelDto;
    }

    /**
     * modelDto转为modelPo
     * @param modelDto
     * @param id
     * @return
     */
    private ModelPo convert(ModelDto modelDto, Long id) {
        ModelPo modelPo = new ModelPo();
        modelPo.setId(modelDto.getId());
        modelPo.setTagId(id);
        modelPo.setName(modelDto.getName());
        modelPo.setMainClass(modelDto.getMainClass());
        modelPo.setPath(modelDto.getPath());
        modelPo.setSchedule(modelDto.getSchedule().toPattern());
        modelPo.setCtime(new Date());
        modelPo.setUtime(new Date());
        modelPo.setState(modelDto.getState());
        modelPo.setArgs(modelDto.getArgs());
        return modelPo;
    }



    /**
     * po转换为dto对象
     * @param tagPo
     * @return
     */
    private TagDto convert(TagPo tagPo){
        TagDto tagDto = new TagDto();
        tagDto.setId(tagPo.getId());
        tagDto.setLevel(tagPo.getLevel());
        tagDto.setName(tagPo.getName());
        tagDto.setPid(tagPo.getPid());
        tagDto.setRule(tagPo.getRule());
        return tagDto;
    }


    /**
     * 将TagDto转换为TagPo对象
     * @return
     */
    private TagPo convert(TagDto tagDto) {
        TagPo tagPo = new TagPo();
        tagPo.setId(tagDto.getId());
        tagPo.setName(tagDto.getName());
        tagPo.setRule(tagDto.getRule());
        tagPo.setLevel(tagDto.getLevel());
        if (tagDto.getLevel() == 1) {
            //如果当前等级为1级,那么设置父ID为-1
            tagPo.setPid(-1L);
        } else {
            tagPo.setPid(tagDto.getPid());
        }
        tagPo.setCtime(new Date());
        tagPo.setUtime(new Date());
        return tagPo;
    }



}
