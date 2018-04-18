package com.roncoo.recharge.web.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.roncoo.recharge.common.entity.DriverLocation;
import com.roncoo.recharge.common.model.MatchModel;
import com.roncoo.recharge.util.GeoHashUtil;
import com.roncoo.recharge.util.ObjectConvert;
import com.roncoo.recharge.util.base.Result;
import com.roncoo.recharge.web.bean.qo.DriverLocationQO;
import com.roncoo.recharge.web.bean.qo.DriverSendLocationQO;
import com.roncoo.recharge.web.bean.qo.LocationQO;
import com.roncoo.recharge.web.bean.qo.RequestInfoQO;
import com.roncoo.recharge.web.bean.req.DispatchReq;
import com.roncoo.recharge.web.bean.res.MatchRes;
import com.roncoo.recharge.web.bean.res.RequestInfoReq;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 调度服务
 * @author xierongli
 * @version $$Id: uber-system, v 0.1 2018/4/9 下午4:19 mark1xie Exp $$
 */
@Component
public class DispatchService {

    @Autowired
    private DriverLocationService driverLocationService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private RequestInfoService requestInfoService;

    /**
     * 调用算法(最短距离)
     * @param
     * @return
     * @author xierongli
     * @date 2018/4/17 下午5:22
     */
    public Result<MatchRes> dispatch(DispatchReq dispatchReq){
        //得到这点的hash值
        GeoHashUtil g = new GeoHashUtil(dispatchReq.getLatitude(),dispatchReq.getLongitude());
        String initGeo = g.getGeoHashBase32();
        //取出相邻八个区域
        List<String> neighbors = g.getGeoHashBase32For9();
        //取出2km内所有符合要求的司机
        List<MatchModel> allDrivers = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(neighbors)){
            List<MatchModel> initModel = locationService.queryByGeoHash(initGeo);
            allDrivers.addAll(initModel);
            for(String geo : neighbors){
                List<MatchModel> matchModels = locationService.queryByGeoHash(geo.substring(0,4));
                allDrivers.addAll(matchModels);
            }
        }
        List<MatchRes> matchResList = ObjectConvert.convertList(allDrivers,MatchRes.class);
        System.out.println(JSON.toJSONString(matchResList));
        //计算距离并排序
        //选取符合条件的车辆
        return null;
    }


    public void insertDriverLocation(DriverSendLocationQO driverSendLocationQO){
        //插入location
        GeoHashUtil g = new GeoHashUtil(driverSendLocationQO.getLatitude(),driverSendLocationQO.getLongitude());
        String hashCode = g.getGeoHashBase32();

        LocationQO locationQO = new LocationQO();
        locationQO.setAddress(driverSendLocationQO.getAddress());
        locationQO.setLatitude(driverSendLocationQO.getLatitude().toString());
        locationQO.setLongitude(driverSendLocationQO.getLongitude().toString());
        locationQO.setGeoHash(hashCode);
        Long locationId = locationService.save(locationQO);
        //判断司机是否上传过位置
        List<DriverLocation> driverLocationList = driverLocationService.queryForList(driverSendLocationQO.getDriverId());
        if(!driverLocationList.isEmpty()){
            DriverLocation driverLocation = driverLocationList.get(0);
            DriverLocationQO driverLocationQO = new DriverLocationQO();
            BeanUtils.copyProperties(driverLocationQO, driverLocation);
            driverLocationQO.setLocationId(locationId);
            driverLocationService.updateById(driverLocationQO);
        }else{
            //插入driver_location
            DriverLocationQO driverLocationQO = new DriverLocationQO();
            driverLocationQO.setDriverId(driverSendLocationQO.getDriverId());
            driverLocationQO.setLocationId(locationId);
            driverLocationQO.setStatus(1);
            driverLocationService.save(driverLocationQO);
        }
    }


    public void insertRequestInfo(RequestInfoReq requestInfoReq){
        //插入location
        GeoHashUtil departureGeo = new GeoHashUtil(requestInfoReq.getDepartureLat(),requestInfoReq.getDepartureLng());
        String departureHashCode = departureGeo.getGeoHashBase32();
        GeoHashUtil destinationGeo = new GeoHashUtil(requestInfoReq.getDestinationLat(),requestInfoReq.getDestinationLng());
        String destinationHashCode = destinationGeo.getGeoHashBase32();

        //departure
        LocationQO locationQO = new LocationQO();
        locationQO.setAddress(requestInfoReq.getDepartureAddress());
        locationQO.setLatitude(requestInfoReq.getDepartureLat().toString());
        locationQO.setLongitude(requestInfoReq.getDepartureLng().toString());
        locationQO.setGeoHash(departureHashCode);
        Long departureId = locationService.save(locationQO);

        //destination
        LocationQO locationQO1 = new LocationQO();
        locationQO1.setAddress(requestInfoReq.getDestinationAddress());
        locationQO1.setLatitude(requestInfoReq.getDestinationLat().toString());
        locationQO1.setLongitude(requestInfoReq.getDestinationLng().toString());
        locationQO1.setGeoHash(destinationHashCode);
        Long destinationId = locationService.save(locationQO1);

        //插入request_info
        RequestInfoQO requestInfoQO = new RequestInfoQO();
        requestInfoQO.setPassengerId(requestInfoReq.getPassengerId());
        requestInfoQO.setDepartureId(departureId);
        requestInfoQO.setDestinationId(destinationId);
        requestInfoQO.setSurge(0);
        requestInfoService.save(requestInfoQO);
    }







}
