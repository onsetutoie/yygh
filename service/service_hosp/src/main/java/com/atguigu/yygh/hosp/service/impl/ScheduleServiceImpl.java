package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    //上传排班
    @Override
    public void save(Map<String, Object> paramMap) {
        //转化参数
        Schedule schedule = JSONObject.parseObject(JSONObject.toJSONString(paramMap), Schedule.class);
        Schedule targetSchedule = scheduleRepository.getByHoscodeAndHosScheduleId(
                (String)paramMap.get("hoscode"),(String)paramMap.get("hosScheduleId")
        );
        //存在更新
        if (targetSchedule != null) {
            schedule.setId(targetSchedule.getId());
            schedule.setCreateTime(targetSchedule.getCreateTime());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            scheduleRepository.save(schedule);
        }else {
            //不存在 新增
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            scheduleRepository.save(schedule);
        }
    }

    //带条件分页查询排班
    @Override
    public Page<Schedule> selectPage(int page, int limit, ScheduleQueryVo scheduleQueryVo) {
        //创建分页对象 顺序 页码
        Sort sort = Sort.by(Sort.Direction.DESC,"workDate");
        Pageable pageable = PageRequest.of(page-1,limit,sort);

        //创建条件模板
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(scheduleQueryVo,schedule);
        schedule.setIsDeleted(0);

        //创建匹配器，即如何使用查询条件
        ExampleMatcher matcher = ExampleMatcher.matching() //构建对象
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)//改变默认字符串匹配方式：模糊查询
                .withIgnoreCase(true); //改变默认大小写忽略方式：忽略大小写
        Example<Schedule> example = Example.of(schedule,matcher);

        //调用接口方法
        Page<Schedule> pageInfo = scheduleRepository.findAll(example, pageable);
        return pageInfo;
    }

    //删除排版
    @Override
    public void remove(String hoscode, String hosScheduleId) {
        Schedule schedule = scheduleRepository.getByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if (schedule == null) {
            throw new YyghException(20001,"排班编号有误");
        }
        scheduleRepository.deleteById(schedule.getId());
    }

    //根据医院编号 和 科室编号 ，查询排班规则数据
    @Override
    public Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode) {

        //1. 创建返回对象
        Map<String, Object> result = new HashMap<>();
        //2. 带条件带分页的聚合查询 （list）
        //2.1 创建查询条件对象
        Criteria criteria = Criteria.where("hoscode").is(hoscode)
                .and("depcode").is(depcode);
        //2.2 创建聚合查询对象
        Aggregation agg = Aggregation.newAggregation(
                //2.2.1 设置查询条件
                Aggregation.match(criteria),
                //2.2.2 设置聚合参数+聚合查询字段
                Aggregation.group("workDate").first("workDate").as("workDate")
                        .count().as("docCount")
                .sum("reservedNumber").as("reservedNumber")
                .sum("availableNumber").as("availableNumber"),
                //2.2.3 排序
                Aggregation.sort(Sort.Direction.ASC,"workDate"),
                //2.2.4分页
                Aggregation.skip((page-1)*limit),
                Aggregation.limit(limit)
        );
        //2.3进行聚合查询
        AggregationResults<BookingScheduleRuleVo> aggregate =
                mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> bookingScheduleRuleList = aggregate.getMappedResults();
        //3. 带条件的聚合查询 (total)
        //3.1创建聚合查询对象
        Aggregation aggTotal = Aggregation.newAggregation(
                //3.1.1 设置查询条件
                Aggregation.match(criteria),
                //3.1.2 设置聚合参数+聚合查询字段
                Aggregation.group("workDate"));
        //3.2 进行聚合查询
        AggregationResults<BookingScheduleRuleVo> aggregateTotal =
                mongoTemplate.aggregate(aggTotal, Schedule.class, BookingScheduleRuleVo.class);
        //3.3 获取total
        List<BookingScheduleRuleVo> mappedResults = aggregateTotal.getMappedResults();
        int total = mappedResults.size();

        //4. 遍历数据，换算星期
        for (BookingScheduleRuleVo bookingScheduleRuleVo : bookingScheduleRuleList) {
            Date workDate = bookingScheduleRuleVo.getWorkDate();
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
        }

        //5. 封装数据并返回
        result.put("bookingScheduleRuleList",bookingScheduleRuleList);
        result.put("total",total);

        //获取医院名称
        Hospital hospital = hospitalService.getHospital(hoscode);
        String hosname = hospital.getHosname();
        //其他基础数据
        Map<String,String> baseMap = new HashMap<>();
        baseMap.put("hosname",hosname);
        result.put("baseMap",baseMap);

        return result;
    }

    //根据医院编号 、科室编号和工作日期，查询排班详细信息
    @Override
    public List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate) {
        //1查询排班数据
        List<Schedule> list =
            scheduleRepository.getByHoscodeAndDepcodeAndWorkDate(hoscode, depcode, new DateTime(workDate).toDate());

        //2.翻译字段 排班详情其他值 医院名称、科室名称、日期对应星期
        list.stream().forEach(item ->{
            this.packageSchedule(item);
        });
        return list;
    }

    //获取可预约排班数据
    @Override
    public Map<String, Object> getBookingSchedule(Integer page, Integer limit, String hoscode, String depcode) {
        Map<String, Object> result = new HashMap<>();

        //1根据hoscode查询医院信息，获取预约规则
        Hospital hospital = hospitalService.getHospital(hoscode);
        if(hospital==null){
            throw new YyghException(20001,"医院信息有误");
        }
        BookingRule bookingRule = hospital.getBookingRule();

        //2根据预约规则、分页信息查询可预约日期集合分页对象(IPage<Date>)
        IPage<Date> iPage = this.getDateListPage(page,limit,bookingRule);
        List<Date> dataPageList = iPage.getRecords();

        //3参考后台接口实现聚合查询（List<BookingScheduleRuleVo>）
        //3.1 创建查询条件对象
        Criteria criteria = Criteria.where("hoscode").is(hoscode).
                and("depcode").is(depcode).and("workDate").in(dataPageList);
        //3.2 创建聚合查询对象
        Aggregation agg = Aggregation.newAggregation(
                //3.2.1 设置查询条件
                Aggregation.match(criteria),
                //3.2.2 设置聚合参数，聚合查询字段
                Aggregation.group("workDate").first("workDate").as("workDate")
                .count().as("docCount")
                .sum("reservedNumber").as("reservedNumber")
                .sum("availableNumber").as("availableNumber")
        );
        //3.3执行聚合查询List<BookingScheduleRuleVo>
        AggregationResults<BookingScheduleRuleVo> aggregate =
                mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> scheduleVoList  = aggregate.getMappedResults();
        //3.4转化查询结果类型，List=>Map k:workDate v: BookingScheduleRuleVo
        Map<Date,BookingScheduleRuleVo> scheduleVoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(scheduleVoList)){
            scheduleVoMap = scheduleVoList.stream().collect(Collectors.toMap(
                    BookingScheduleRuleVo::getWorkDate,
                    BookingScheduleRuleVo -> BookingScheduleRuleVo
            ));
        }
        //4合并步骤2和步骤3数据
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        for (int i = 0, let = dataPageList.size(); i < let; i++) {
            //4.1遍历datePageList，取出每一天日期
            Date date = dataPageList.get(i);
            //4.2根据日期查询scheduleVoMap，获取排班聚合的记录信息
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleVoMap.get(date);
            //4.3排班聚合的记录是空的，需要初始化
            if (bookingScheduleRuleVo == null) {
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                bookingScheduleRuleVo.setDocCount(0);
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            //4.4设置排班日期
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //4.5根据日期换算周几
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
            //4.6根据时间进行记录状态判断
            //状态 0：正常 1：即将放号 -1：当天已停止挂号
            //最后一页，最后一条记录，状态为 1：即将放号
            if (i == let && page == iPage.getPages()) {
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }
            //第一页，第一条记录，如果已过停止挂号时间，状态为-1：当天已停止挂号
            if (i == 0 && page == 1) {
                DateTime stopDateTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if (stopDateTime.isBeforeNow()) {
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }
        //5封装数据
        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHospital(hoscode).getHosname());
        //科室
        Department department =departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;

    }

    @Override
    public Schedule findScheduleById(String id) {
        Schedule schedule = scheduleRepository.findById(id).get();
        return this.packageSchedule(schedule);
    }

    //根据排班id获取预约下单数据
    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {
        //1根据排班id获取排班信息
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        if (schedule == null) {
            throw new YyghException(20001,"排班信息有误");
        }
        //2根据hoscode查询医院信息
        Hospital hospital = hospitalService.getHospital(schedule.getHoscode());
        if (hospital == null) {
            throw new YyghException(20001,"医院信息有误");
        }
        //3从医院信息取出预约规则
        BookingRule bookingRule = hospital.getBookingRule();
        if (bookingRule == null) {
            throw new YyghException(20001,"预约规则有误");
        }
        //4封装基础数据
        ScheduleOrderVo scheduleOrderVo = new ScheduleOrderVo();
        scheduleOrderVo.setHoscode(hospital.getHoscode());
        scheduleOrderVo.setHosname(hospital.getHosname());
        scheduleOrderVo.setDepcode(schedule.getDepcode());
        scheduleOrderVo.setDepname(departmentService.getDepName(hospital.getHoscode(),schedule.getDepcode()));
        scheduleOrderVo.setHosScheduleId(schedule.getHosScheduleId());
        scheduleOrderVo.setAvailableNumber(schedule.getAvailableNumber());
        scheduleOrderVo.setTitle(schedule.getTitle());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());
        scheduleOrderVo.setAmount(schedule.getAmount());

        //5封装根据预约规则推算的时间信息
        //5.1推算可以退号的截止日期+时间
        //退号截止天数（如：就诊前一天为-1，当天为0）
        DateTime quitDate = new DateTime(schedule.getWorkDate())
                .plusDays(bookingRule.getQuitDay());
        DateTime quitDateTime = this.getDateTime(quitDate.toDate(),bookingRule.getQuitTime());
        scheduleOrderVo.setQuitTime(quitDateTime.toDate());

        //5.2预约开始时间
        DateTime startDateTime = this.getDateTime(new Date(),bookingRule.getReleaseTime());
        scheduleOrderVo.setStartTime(startDateTime.toDate());

        //5.3 预约截至时间
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate()
                , bookingRule.getStopTime());
        scheduleOrderVo.setEndTime(endTime.toDate());

        //5.4当天停止挂号时间
        DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
        scheduleOrderVo.setStopTime(stopTime.toDate());
        return scheduleOrderVo;

    }

    //根据预约规则、分页信息查询可预约日期集合分页对象(IPage<Date>)
    private IPage<Date> getDateListPage(Integer page, Integer limit, BookingRule bookingRule) {
        //1从预约规则中获取开始挂号时间（当前系统日期+开始时间）
        DateTime releaseDateTime =
                this.getDateTime(new Date(),bookingRule.getReleaseTime());
        //2从预约规则中获取周期，判断周期是否需要+1
        Integer cycle = bookingRule.getCycle();
        if(releaseDateTime.isBeforeNow()) cycle+=1;
        //3根据周期推算出可以挂号日期，存入集合（List）
        List<Date> dateList = new ArrayList<>();
        for (int i = 0; i < cycle; i++) {
            DateTime plusDays = new DateTime().plusDays(i);
            String plusDaysString = plusDays.toString("yyyy-MM-dd");
            dateList.add(new DateTime(plusDaysString).toDate());
        }
        //4准备分页参数
        int start = (page-1)*limit;
        int end = Math.min((page-1)*limit+limit,dateList.size());
        //5根据参数获取分页后日期集合
        List<Date> datePageList = new ArrayList<>();
        for (int i = start; i < end; i++) {
            datePageList.add(dateList.get(i));
        }
        //6封装数据到IPage对象，返回
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page,limit,dateList.size());
        iPage.setRecords(datePageList);
        return iPage;
    }

    //1从预约规则中获取开始挂号时间（当前系统日期+开始时间）
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString  =
                new DateTime(date).toString("yyyy-MM-dd")+" "+ timeString;
        DateTime dateTime =
                DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }

    //封装排班详情其他值 医院名称、科室名称、日期对应星期
    private Schedule packageSchedule(Schedule schedule) {
        //设置医院名称
        schedule.getParam()
                .put("hosname",hospitalService.getHospital(schedule.getHoscode()).getHosname());
        //设置科室名称
        schedule.getParam().put("depname",
                departmentService.getDepName(schedule.getHoscode(),schedule.getDepcode()));
        //设置日期对应星期
        schedule.getParam().put("dayOfWeek",this.getDayOfWeek(new DateTime(schedule.getWorkDate())));

        return schedule;
    }

    /**
     * 根据日期获取周几数据
     * @param dateTime
     * @return
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }


}
