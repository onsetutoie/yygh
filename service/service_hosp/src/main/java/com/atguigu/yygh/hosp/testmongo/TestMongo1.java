package com.atguigu.yygh.hosp.testmongo;


import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/mongo1")
public class TestMongo1 {

    @Autowired
    private MongoTemplate mongoTemplate;

    //添加
    @GetMapping("create")
    public void createUser() {
        User user = new User();
        user.setAge(20);
        user.setName("test");
        user.setEmail("4932200@qq.com");
        User user1 = mongoTemplate.insert(user);
        System.out.println(user1);
    }

    //查询所有
    @GetMapping("findAll")
    public void findUser() {
        List<User> userList = mongoTemplate.findAll(User.class);
        System.out.println(userList);
    }

    //根据id查询
    @GetMapping("findId")
    public void getById(){
        User user = mongoTemplate.findById("62d7dd9a910d841baf70d74a", User.class);
        System.out.println("user = " + user);
    }

    //条件查询
    @GetMapping("findUser")
    public void findUserList(){
        Query query = new Query(Criteria.where("name").is("test")
                                    .and("age").is(20));
        List<User> userList = mongoTemplate.find(query, User.class);
        System.out.println(userList);
    }

    //模糊查询
    @GetMapping("findLike")
    public void findLikeName(){
        String name = "est";
        String regex = String.format("%s%s%s", "^.*", name, ".*$");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("name").regex(pattern));
        List<User> userList = mongoTemplate.find(query, User.class);
        System.out.println(userList);
    }

    //分页查询
    @GetMapping("findPage")
    public void findUsersPage(){
        String name = "est";
        int pageNo = 1;
        int pageSize = 10;

        Query query = new Query();
        String regex = String.format("%s%s%s", "^.*", name, ".*$");
        Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);

        query.addCriteria(Criteria.where("name").regex(pattern));
        long totalCount = mongoTemplate.count(query, User.class);

        List<User> users = mongoTemplate.find(query.skip((pageNo - 1) * pageSize)
                .limit(pageSize), User.class);

        Map<String, Object> pageMap = new HashMap<>();
        pageMap.put("list", users);
        pageMap.put("totalCount",totalCount);
        System.out.println(pageMap);
    }

    //修改
    @GetMapping("update")
    public void updateUser() {
        User user = mongoTemplate.findById("62d7dd9a910d841baf70d74a", User.class);
        user.setName("test_1");
        user.setAge(25);
        user.setEmail("493220990@qq.com");
        Query query = new Query(Criteria.where("_id").is(user.getId()));
        Update update = new Update();
        update.set("name", user.getName());
        update.set("age", user.getAge());
        update.set("email", user.getEmail());
        UpdateResult result = mongoTemplate.upsert(query, update, User.class);
        long count = result.getModifiedCount();
        System.out.println(count);
    }

    //删除操作
    @GetMapping("delete")
    public void delete() {
        Query query =
                new Query(Criteria.where("_id").is("62d7dd9a910d841baf70d74a"));
        DeleteResult result = mongoTemplate.remove(query, User.class);
        long count = result.getDeletedCount();
        System.out.println(count);
    }


}
