package com.atguigu.yygh.hosp.testmongo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mongo2")
public class TestMongo2 {

    @Autowired
    private UserRepository userRepository;

    //新增
    @GetMapping("create")
    public void create(){
        User user = new User();
        user.setAge(24);
        user.setName("张三");
        user.setEmail("4932200111@qq.com");

        User save = userRepository.save(user);
        System.out.println("user = " + save);
    }

    //查询所有
    @GetMapping("findAll")
    public void findUser(){
        List<User> all = userRepository.findAll();
        System.out.println("all = " + all);
    }

    //id查询
    @GetMapping("findId")
    public void findId(){
        User user = userRepository.findById("62d7ec8682a95075af6ff2fe").get();
        System.out.println("user = " + user);
    }

    //条件查询
    @GetMapping("findQuery")
    public void findUserList(){
        User user = new User();
        user.setName("张三");
        user.setAge(24);

        Example<User> usersExample = Example.of(user);

        List<User> users = userRepository.findAll(usersExample);
        System.out.println("users = " + users);
    }

    //模糊查询
    @GetMapping("findLike")
    public void findUserLike(){

        ExampleMatcher matcher = ExampleMatcher.matching()//构建对象
                //改变默认字符串匹配方式：模糊查询
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);//改变默认大小写忽略方式：忽略大小写

        User user = new User();
        user.setName("三");
        Example<User> usersExample = Example.of(user,matcher);
        List<User> users = userRepository.findAll(usersExample);

        System.out.println("users = " + users);
    }

    //分页查询
    @GetMapping("findPage")
    public void findPage(){
        Sort sort = Sort.by(Sort.Direction.DESC,"age");
        Pageable pageable = PageRequest.of(0, 10, sort);

        ExampleMatcher matcher = ExampleMatcher.matching()//构建对象
                //改变默认字符串匹配方式：模糊查询
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);//改变默认大小写忽略方式：忽略大小写

        User user = new User();
        user.setName("三");
        Example<User> usersExample = Example.of(user,matcher);
        Page<User> pages = userRepository.findAll(usersExample, pageable);
        System.out.println("pages = " + pages);

    }

    //修改
    @GetMapping("update")
    public void updateUser(){
        User user = userRepository.findById("62d7ec9882a95075af6ff2ff").get();
        user.setName("张三_1");
        user.setAge(25);
        user.setEmail("883220990@qq.com");
        User save = userRepository.save(user);
        System.out.println("save = " + save);
    }

    //删除
    @GetMapping("delete")
    public void delete() {
        userRepository.deleteById("62d7ec8682a95075af6ff2fe");
    }

    @GetMapping("getQuery")
    public void getByNameAndAge(){
        List<User> users = userRepository.getByNameAndAge("张三",24);
        System.out.println("users = " + users);

    }

    @GetMapping("getLike")
    public void getLike(){
        List<User> users = userRepository.getByNameLike("三");
        System.out.println("users = " + users);

    }



}
