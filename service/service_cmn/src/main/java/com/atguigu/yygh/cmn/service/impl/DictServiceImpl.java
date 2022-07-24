package com.atguigu.yygh.cmn.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.yygh.cmn.listener.DictListener;
import com.atguigu.yygh.cmn.mapper.DictMapper;
import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;



import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Autowired
    private DictListener dictListener;

    //根据数据id查询子数据列表
    @Cacheable(value = "dict", key = "'selectIndexList'+#id")
    @Override
    public List<Dict> findChildData(Long id) {
        //1.根据父id查询子数据集合
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id",id);
        List<Dict> dictList = baseMapper.selectList(wrapper);
        //2.遍历查询是否有子数据
        for (Dict dict : dictList) {
            boolean hasChild = this.isChild(dict.getId());
            dict.setHasChildren(hasChild);
        }
        return dictList;
    }

    //导出数据列表
    @Override
    public void exportData(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("UTF-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode("数据字典","UTF-8");
            response.setHeader("Content-disposition", "attachment;filename="+ fileName + ".xlsx");

            List<Dict> dictList = baseMapper.selectList(null);
            List<DictEeVo> dictEeVoList = new ArrayList<>();

            for (Dict dict : dictList) {
                DictEeVo dictEeVo = new DictEeVo();
                BeanUtils.copyProperties(dict,dictEeVo);
                dictEeVoList.add(dictEeVo);
            }

            EasyExcel.write(response.getOutputStream(),DictEeVo.class)
                    .sheet("数据字典").doWrite(dictEeVoList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //导入字典数据
    @Override
    public void importData(MultipartFile file) {

        try {
            InputStream inputStream = file.getInputStream();
            EasyExcel.read(inputStream,DictEeVo.class,dictListener)
                    .sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
            throw new YyghException(20001,"导入数据失败");
        }
    }

    //获取数据字典名称
    @Override
    public String getNameByInfo(String parentDictCode, String value) {
        //1.判断parentDictCode是否为空
        if (StringUtils.isEmpty(parentDictCode)) {
            //2.国标数据查询
            Dict dict = baseMapper.selectOne(new QueryWrapper<Dict>()
                    .eq("value",value));
            if (dict != null) {
                return dict.getName();
            }
        }else {
            //2. 自定义数据查询
            Dict parentDict  = this.getDictByDictCode(parentDictCode);

            Dict dict = baseMapper.selectOne(new QueryWrapper<Dict>().eq("parent_id",
                    parentDict.getId()).eq("value", value));
            if(null != dict) {
                return dict.getName();
            }
        }
        return "";
    }

    //根据dictCode获取下级节点
    @Override
    public List<Dict> findByDictCode(String dictCode) {
        Dict dict = this.getDictByDictCode(dictCode);
        if(null == dict) return null;
        List<Dict> childData = this.findChildData(dict.getId());
        return childData;
    }

    //根据字典编码查询父级别数据
    private Dict getDictByDictCode(String parentDictCode) {
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_code",parentDictCode);
        Dict dict = baseMapper.selectOne(wrapper);
        return dict;
    }

    //查询是否有子数据
    private Boolean isChild(Long id){
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id",id);
        Integer childCount = baseMapper.selectCount(wrapper);
        return childCount>0;
    }

}
