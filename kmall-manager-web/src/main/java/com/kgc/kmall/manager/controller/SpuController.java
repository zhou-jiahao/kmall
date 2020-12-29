package com.kgc.kmall.manager.controller;

import com.kgc.kmall.bean.PmsBaseSaleAttr;
import com.kgc.kmall.bean.PmsProductImage;
import com.kgc.kmall.bean.PmsProductInfo;
import com.kgc.kmall.bean.PmsProductSaleAttr;
import com.kgc.kmall.service.SpuService;
import org.apache.commons.io.FilenameUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
public class SpuController {

    @Reference
    SpuService spuService;



    @RequestMapping("/spuList")
    public List<PmsProductInfo> pmsProductInfo(Long catalog3Id){
        return spuService.spuList(catalog3Id);
    }


    @RequestMapping("/fileUpload")
    public String fileUpload(@RequestParam("file")MultipartFile file){
        try {
            //文件上传
            //返回文件上传后的路径
            String comFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init("D:\\第三期\\IdeaProjects\\kmall-zjh\\kmall-manager-web\\target\\classes\\tracker.conf");
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getTrackerServer();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String[] upload_file = storageClient.upload_file(file.getBytes(), extension, null);
            String path="http://192.168.56.138";
            for (int i = 0; i < upload_file.length; i++) {
                String s = upload_file[i];
                System.out.println("s = " + s);
                path+="/"+s;
            }
            System.out.println(path);
            return path;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @RequestMapping("/baseSaleAttrList")
    public List<PmsBaseSaleAttr> baseSaleAttrList(){
        List<PmsBaseSaleAttr> saleAttrList = spuService.baseSaleAttrList();
        return saleAttrList;
    }

    @RequestMapping("/saveSpuInfo")
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){
        //添加spu
        //保存数据库
        Integer integer = spuService.saveSpuInfo(pmsProductInfo);
        return integer>0?"success":"fail";
    }


    @RequestMapping("/spuSaleAttrList")
    public List<PmsProductSaleAttr> spuSaleAttrList(Long spuId){
        List<PmsProductSaleAttr> pmsProductSaleAttrList=spuService.spuSaleAttrList(spuId);
        return pmsProductSaleAttrList;
    }


    @RequestMapping("/spuImageList")
    public List<PmsProductImage> spuImageList(Long spuId){
        List<PmsProductImage> pmsProductImageList = spuService.spuImageList(spuId);
        return pmsProductImageList;
    }
}
