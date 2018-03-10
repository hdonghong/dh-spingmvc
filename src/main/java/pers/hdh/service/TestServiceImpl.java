package pers.hdh.service;

import pers.hdh.annotation.DhService;

@DhService("testService")
public class TestServiceImpl implements TestService {

    @Override
    public void doServiceTest() {
        System.out.println("业务层执行方法了。。。");
    }
}
