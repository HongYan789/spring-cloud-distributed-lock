### spring-cloud-distributed-lock



##### 该项目主要实现了三类分布式锁以及实现自定义注解形式的分布式锁

分布式锁一般有三种实现方式：**1. 数据库锁；2. 基于Redis的分布式锁；3. 基于ZooKeeper的分布式锁**



分布式锁应该是怎么样的？

- 互斥性 可以保证在分布式部署的应用集群中，同一个方法在同一时间只能被一台机器上的一个线程执行。
- 这把锁要是一把可重入锁（避免死锁）
- 不会发生死锁：有一个客户端在持有锁的过程中崩溃而没有解锁，也能保证其他客户端能够加锁
- 这把锁最好是一把阻塞锁（根据业务需求考虑要不要这条）
- 有高可用的获取锁和释放锁功能
- 获取锁和释放锁的性能要好



#### 数据库锁

##### 基于新增数据库表

```
CREATE TABLE `methodLock` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `method_name` varchar(64) NOT NULL DEFAULT '' COMMENT '锁定的方法名',
  `desc` varchar(1024) NOT NULL DEFAULT '备注信息',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '保存数据时间，自动生成',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_method_name` (`method_name `) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='锁定中的方法';
```



新增实体类

```
@Data
public class MethodLock {

    private Integer id;

    private String methodName;

    private String desc;

    private Date updateTime;
}
```





新增对应mapper类,主要实现插入（加锁）、删除（解锁）方法

```
public interface MethodLockMapper {
    @Insert({"insert into method_lock set method_name=#{methodName} ,`desc`=#{desc}"})
    int save(MethodLock methodLock);

    @Delete({"delete from method_lock where method_name=#{methodName}"})
    int delete(String methodName);
}
```





新增并发调用controller

```
/**
     * method 1：
     * 基于数据库锁方式实现分布式锁
     * @return
     */
    @GetMapping("/lock1")
    public Boolean getLock1(){
        Boolean flag = true;
        String methodName = "lockMethod";
        //模拟加锁
        try {
            log.info(">>>>执行database加锁操作>>>>，threadName:[{}]",Thread.currentThread().getName());
            lock(methodName,"lock method by database");
        } catch (Exception e) {
            flag = false;
            log.error(">>>>执行database加锁操作失败>>>>，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
        }
        if(flag){
            try {
                //模拟业务操作
                log.info(">>>>休眠15s模拟业务处理耗时>>>>，threadName:[{}]",Thread.currentThread().getName());
                Thread.sleep(15000);
                //模拟解锁
                unLock(methodName);
                log.info(">>>>解锁成功>>>>，threadName:[{}]",Thread.currentThread().getName());
            } catch (Exception e) {
                log.error(">>>>执行database加锁操作失败>>>>，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
            }
        }
        return flag;
    }
    /**
     * 模拟加锁
     */
    private void lock(String methodName,String desc) {
        MethodLock lock = new MethodLock();
        lock.setMethodName(methodName);
        lock.setDesc(desc);
        methodLockMapper.save(lock);
    }
    /**
     * 模拟解锁
     * @param methodName
     */
    private void unLock(String methodName) {
        methodLockMapper.delete(methodName);
    }
```



​	

##### 基于数据库乐观锁

基于时间戳（timestamp）记录机制实现

实现原理：给数据库表增加一个时间戳字段类型的字段，当读取数据时，将timestamp字段的值一同读出，数据每更新一次，timestamp也同步更新。当对数据做提交更新操作时，检查当前数据库中数据的时间戳和自己更新前取到的时间戳进行对比，若相等，则更新，否则认为是失效数据。

实现步骤：

1. 新增数据表

```
--用于实现数据库乐观锁
drop table if exists `user_lock`;
CREATE TABLE `user_lock` (
  `user_id` bigint(19) NOT NULL  AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(45) DEFAULT NULL,
  `version` int(11) NOT NULL  COMMENT '版本号',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '时间搓',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `lock`.`user_lock`(`user_id`, `user_name`, `version`, `timestamp`) VALUES (150, 'zhangsan', 1, '2020-09-25 07:05:11');
```



2. 增加实体类bean

```
@Data
public class UserLock {

    private Long userId;

    private String userName;

    private Integer version;

    private Date timestamp;
}
```





3. 增加相应mapper类并实现3个方法调用，分别是通过主键id查询当前数据对象(其中包含version版本号/timestamp时间搓字段)、通过主键id+version实现对象更新、通过主键id+timestamp实现对象更新方法

```
public interface UserLockMapper {

    /**
     * databse查询增加乐观锁
     * @param userId
     * @return
     */
    @Select({" select user_id as userId,user_name as userName,`version`,`timestamp` from user_lock where user_id =#{userId}"})
    UserLock query(Integer userId);

    /**
     * 根据主键id+版本号实现乐观锁，其中版本号为上一步select查询出来的version，如果与数据库中相等则会修改成功，否则修改失败
     * @param userLock
     * @return
     */
    @Update({" update user_lock set user_name=#{userName},`version`=`version`+1 where user_id=#{userId} and `version`=#{version} "})
    Integer updateOptimisticLockByVersion(UserLock userLock);

    /**
     * 根据主键id+时间搓实现乐观锁，其中时间搓为上一步select查询出来的time，如果与数据库中相等则会修改成功，否则修改失败
     * @param userLock
     * @return
     */
    @Update({" update user_lock set user_name=#{userName} where user_id=#{userId} and `timestamp`=#{timestamp} "})
    Integer updateOptimisticLockByTimeStamp(UserLock userLock);
}
```





4. 新增并发调用controller

```
/**
     * method 3：
     * 基于数据库乐观锁实现分布式锁
     * @return
     */
    @GetMapping("/lock3")
    public Boolean getLock3(Integer userId){
        //获取欲更新的数据
        UserLock userLock = userLockMapper.query(userId);
        log.info("查询出修改前database数据:UserLock:[{}]",userLock);
        //模拟业务操作
        try {
            log.info(">>>>休眠15s模拟业务处理耗时，threadName:[{}]>>>>",Thread.currentThread().getName());
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            log.error("业务处理异常",e);
        }
        //采用version版本号方式实现分布式锁
				//return lockByOptimisticVersion(userLock);
        //采用timeStamp时间搓方式实现分布式锁
        return lockByOptimistictimeStamp(userLock);
    }

    private Random random = new Random();
    

    /**
     * 采用时间搓timeStamp实现的乐观锁
     * @param userLock
     */
    public Boolean lockByOptimistictimeStamp(UserLock userLock){
        UserLock lock = new UserLock();
        lock.setUserId(userLock.getUserId());
        lock.setUserName("modifyedByTimeStamp"+random.nextInt(10)+100);
        lock.setVersion(userLock.getVersion());
        lock.setTimestamp(userLock.getTimestamp());
        Integer count = userLockMapper.updateOptimisticLockByTimeStamp(lock);
        Boolean flag = count > 0 ? true :false;
        log.info("查询出修改后database数据:UserLock:[{}],修改操作执行:[{}]",lock,flag);
        return flag;
    }
```







基于版本号（version）的方式实现

方法同上，只需将并发调用controller稍微调整即可

```
/**
     * method 3：
     * 基于数据库乐观锁实现分布式锁
     * @return
     */
    @GetMapping("/lock3")
    public Boolean getLock3(Integer userId){
        //获取欲更新的数据
        UserLock userLock = userLockMapper.query(userId);
        log.info("查询出修改前database数据:UserLock:[{}]",userLock);
        //模拟业务操作
        try {
            log.info(">>>>休眠15s模拟业务处理耗时，threadName:[{}]>>>>",Thread.currentThread().getName());
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            log.error("业务处理异常",e);
        }
        //采用version版本号方式实现分布式锁
        return lockByOptimisticVersion(userLock);
        //采用timeStamp时间搓方式实现分布式锁
        //return lockByOptimistictimeStamp(userLock);
    }

    private Random random = new Random();
    /**
     * 采用版本号version实现的乐观锁
     * @param userLock
     */
    public Boolean lockByOptimisticVersion(UserLock userLock){
        UserLock lock = new UserLock();
        lock.setUserId(userLock.getUserId());
        lock.setUserName("modifyedByVersion"+random.nextInt(10));
        lock.setVersion(userLock.getVersion());
        Integer count = userLockMapper.updateOptimisticLockByVersion(lock);
        Boolean flag = count > 0 ? true :false;
        log.info("查询出修改后database数据:UserLock:[{}],修改操作执行:[{}]",lock,flag);
        return flag;
    }

    
```







##### 基于数据库排他锁(即悲观锁)

select * from table  <font color=red>for update</font>;



具体实现步骤：

1. 新增数据表

```
--用于实现数据库排他锁
drop table if exists `user_info`;
CREATE TABLE `user_info` (
  `user_id` bigint(19) NOT NULL  AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(45) DEFAULT NULL,
  `account` varchar(45) NOT NULL,
  `password` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (150, 'name1', 'Account1', 'pass1');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (152, 'name3', 'Account3', 'pass3');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (154, 'name5', 'Account5', 'pass5');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (156, 'name7', 'Account7', 'pass7');
INSERT INTO `user_info`(`user_id`, `user_name`, `account`, `password`) VALUES (158, 'name9', 'Account9', 'pass9');
```





2. 新增对应mapper类，并实现排他锁的sql查询操作

```
public interface UserInfoMapper {

    @Select({" select user_id as userId,user_name as userName,account,password from user_info order by user_id  "})
    List<UserInfo> queryAll();

    /**
     * databse查询增加排他锁
     * @param userId
     * @return
     */
    @Select({" select user_id as userId,user_name as userName,account,password from user_info where user_id =#{userId} for update"})
    UserInfo queryByExclusiveLock(Integer userId);
}

```



3. 新增并发调用controller(其中需调整为手动模式操作数据)

```
/**
     * method 2：
     * 基于数据库排他锁（悲观锁）实现分布式锁
     * @return
     */
    @GetMapping("/lock2")
    public R getLock2(Integer userId){
        try(Connection conn = dataSource.getConnection();){
            //设置手动提交连接
            //执行数据库排他锁加锁操作
            UserInfo userInfo = lockByExclusive(userId,conn);
            //模拟业务操作
            log.info(">>>>休眠15s模拟业务处理耗时，threadName:[{}]>>>>",Thread.currentThread().getName());
            Thread.sleep(15000);

            //执行数据库排他锁解锁操作
            unLockByExclusive(conn);
            return new R(OpCode.sucess.getCode(),OpCode.sucess.getValue(),userInfo);
        }catch (Exception e){
            log.error("数据库执行异常",e);
            return new R(OpCode.sucess.getCode(),OpCode.sucess.getValue(),new UserInfo());
        }

    }

    /**
     * 排他锁方式加锁
     */
    private UserInfo lockByExclusive(Integer userId,Connection conn){
        try {
            //设置成手动模式提交（mysql默认都是自动提交）
            conn.setAutoCommit(false);
            log.info("执行database数据库加锁操作，threadName:[{}]",Thread.currentThread().getName());
            return userInfoMapper.queryByExclusiveLock(userId);
        } catch (Exception e) {
            log.error("执行database数据库加锁操作，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
            return new UserInfo();
        }
    }

    /**
     * 排他锁方式解锁
     */
    private void unLockByExclusive(Connection conn){
        try {
            log.info("执行database数据库解锁操作，threadName:[{}]",Thread.currentThread().getName());
            //释放锁
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
                log.error("database数据库解锁失败，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),e.getMessage());
            } catch (SQLException ex) {
                log.error("database数据库解锁异常，threadName:[{}]，error:[{}]",Thread.currentThread().getName(),ex.getMessage());
            }
        }
    }

```





#### 基于Redis的分布式锁

1. 引入所需jar包

```
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```



2. 新增配置文件属性

   application.properties

```
## redis
spring.redis.database=0
spring.redis.lettuce.pool.max-idle=5000
spring.redis.lettuce.pool.max-wait=50000
spring.redis.cluster.timeout=50000
spring.redis.cluster.max-redirects=3
spring.redis.cluster.nodes=192.168.19.71:6380,192.168.19.72:6381,192.168.19.73:6382
```

redisLock.lua

```
if redis.call('get',KEYS[1]) == ARGV[1] then
    return redis.call('del',KEYS[1])
else
    return 0
end
```



3. 新增configure配置

```
@Configuration
public class RedisLettuceConfiguration {

    @Autowired
    private Environment environment;

    @Bean
    public RedisTemplate<String, Serializable> redisTemplate() {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setConnectionFactory(lettuceConnectionFactory());
        return template;
    }

    @Bean(name="lettuceConnectionFactory")
    public RedisConnectionFactory lettuceConnectionFactory() {
        Map<String, Object> source = new HashMap<>();
        source.put("spring.redis.cluster.nodes", environment.getProperty("spring.redis.cluster.nodes"));
        source.put("spring.redis.cluster.timeout", environment.getProperty("spring.redis.cluster.timeout"));
        source.put("spring.redis.cluster.max-redirects", environment.getProperty("spring.redis.cluster.max-redirects"));
        RedisClusterConfiguration redisClusterConfiguration;
        redisClusterConfiguration = new RedisClusterConfiguration(new MapPropertySource("RedisClusterConfiguration", source));
        return new LettuceConnectionFactory(redisClusterConfiguration);
    }
}

```



4. 编写redis分布式锁工具类

```
@Component
@Slf4j
public class DistributedLockByRedis {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 加锁，自旋重试三次
     *
     * @param redisLockEntity 锁实体
     * @return
     */
    public boolean lock(RedisLockEntity redisLockEntity) {
        boolean locked = false;
        int tryCount = 3;
        while (!locked && tryCount > 0) {
//            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), 2, TimeUnit.MINUTES);
            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), redisLockEntity.getTimeout(), redisLockEntity.getUnit());

            tryCount--;
            log.info("redis加锁:{},thread:{}",locked,Thread.currentThread().getName());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                log.error("线程被中断:{}" ,Thread.currentThread().getId(), e);
                Thread.currentThread().interrupt();
            }
        }
        return locked;
    }

    /**
     * 加锁，实现一直自旋，直到抢占到锁
     * 抢占到锁分2种情况：
     *  1、上一个线程执行完后主动释放锁后被当前线程抢占到锁
     *  2、上一个线程执行后一直未主动释放锁，则等待redis超时时间自动到期后被当前线程抢占到
     * @param redisLockEntity
     * @return
     */
    public boolean lockSpin(RedisLockEntity redisLockEntity) {
        boolean locked = false;
        while (!locked) {
//            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), 2, TimeUnit.MINUTES);
            locked = redisTemplate.opsForValue().setIfAbsent(redisLockEntity.getLockKey(), redisLockEntity.getRequestId(), redisLockEntity.getTimeout(), redisLockEntity.getUnit());
            log.info("redis加锁:{},thread:{}",locked,Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("线程被中断:{}" ,Thread.currentThread().getId(), e);
                Thread.currentThread().interrupt();
            }
        }
        return locked;
    }

    /**
     * 非原子解锁，可能解别人锁，不安全
     *
     * @param redisLockEntity
     * @return
     */
    public boolean unlock(RedisLockEntity redisLockEntity) {
        if (redisLockEntity == null || redisLockEntity.getLockKey() == null || redisLockEntity.getRequestId() == null){
            return false;
        }
        boolean releaseLock = false;
        String requestId = (String) redisTemplate.opsForValue().get(redisLockEntity.getLockKey());
        if (redisLockEntity.getRequestId().equals(requestId)) {
            releaseLock = redisTemplate.delete(redisLockEntity.getLockKey());
        }
        log.info("redis解锁:{},thread:{}",releaseLock,Thread.currentThread().getName());
        return releaseLock;
    }

    /**
     * 使用lua脚本解锁，不会解除别人锁
     *
     * @param redisLockEntity
     * @return
     */
    public boolean unlockLua(RedisLockEntity redisLockEntity) {
        if (redisLockEntity == null || redisLockEntity.getLockKey() == null || redisLockEntity.getRequestId() == null){
            return false;
        }
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript();
        //用于解锁的lua脚本位置
        redisScript.setLocation(new ClassPathResource("redisLock.lua"));
        redisScript.setResultType(Long.class);
        //没有指定序列化方式，默认使用上面配置的
        Object result = redisTemplate.execute(redisScript, Arrays.asList(redisLockEntity.getLockKey()), redisLockEntity.getRequestId());
        boolean releaseLock = result.equals(Long.valueOf(1));
        log.info("redis解锁:{},thread:{}",releaseLock,Thread.currentThread().getName());
        return releaseLock;
    }

}
```



5. 编写具体的调用类controller

```
@RestController
@RequestMapping("/redis")
@Slf4j
public class RedisController {

    @Autowired
    private DistributedLockByRedis distributedLockByRedis;

    private String redis_key = "data:::redis-lock";

    @GetMapping("/lock1")
    public Boolean getLock1(){
        Boolean flag;
        String requestId = UUID.randomUUID().toString().replace("-","");
        RedisLockEntity entity = new RedisLockEntity(redis_key,requestId);
        distributedLockByRedis.lock(entity);
        try{
            log.info("执行redis-lock1操作，thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(20000);
        }catch (Exception e){

        }finally {
            flag = distributedLockByRedis.unlockLua(entity);
        }
        return flag;
    }

    @GetMapping("/lock2")
    public Boolean getLock2(){
        Boolean flag;
        String requestId = UUID.randomUUID().toString().replace("-","");
        RedisLockEntity entity = new RedisLockEntity(redis_key,requestId);
        distributedLockByRedis.lockSpin(entity);
        try{
            log.info("执行redis-lock2操作，thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(20000);
        }catch (Exception e){

        }finally {
            flag = distributedLockByRedis.unlockLua(entity);
        }
        return flag;
    }
}
```



6. 观察分布式锁调用操作及效果





##### 扩展：改造为自定义注解模式的Redis分布式锁（同理：可改造基于Zookeeper、数据库方式锁）

核心思想：

1. 新增自定义注解

com.hongyan.study.distributedlock.annotation.DistributeLock

```
@Target({ElementType.PARAMETER,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributeLock {

    /**
     * 锁类型
     *
     * @return
     */
    LockType type() default LockType.LOCK;

    /**
     * 分布式锁 名称
     *
     * @return
     */
    String name() default "data:::redis-lock";

    /**
     * 租期、超时时间
     * @return
     */
    long timeout() default -1;

    /**
     * 超时单位
     * @return
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
```



2. 改造redis分布式锁获取锁、解锁工具类

com.hongyan.study.distributedlock.lock.DistributedLockByRedis

同上



3. 新增aop切面用于处理含该自定义注解的分布式锁操作

com.hongyan.study.distributedlock.aop.DistributeLockAop

```
@Aspect
@Component
@Slf4j
public class DistributeLockAop {

    @Autowired
    private DistributedLockByRedis distributedLockByRedis;

    /**
     * 切入点
     */
    @Pointcut("@annotation(com.hongyan.study.distributedlock.annotation.DistributeLock)")
    public void doBusiness() {

    }

    @Around("doBusiness()")
    public R around(ProceedingJoinPoint joinPoint){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取执行方法
        Method method = signature.getMethod();
        // 获取注解信息
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);

        if (null == distributeLock) {
            log.error("不存在该分布式锁");
            return new R(OpCode.InvalidArgument.getCode(),OpCode.InvalidArgument.getValue());
        }
        //获取注解参数值
        String lockName = distributeLock.name(); //锁名称
        LockType type = distributeLock.type(); //锁类型
        long timeout = distributeLock.timeout(); //锁租期失效
        TimeUnit unit = distributeLock.unit(); //锁超时单位

        // 请求的参数
        Object[] args = joinPoint.getArgs();

        //查找是否包含${}字符，如果包含则进行属性值替换操作
        String regex = "\\$\\{(.+?)\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lockName);
        StringBuffer name = new StringBuffer();
        while(matcher.find()){
            log.info(String.format(">>>>>>>>>>>>需要被替换的参数为:[%s]",matcher.group(1)));
            matcher.appendReplacement(name,(String)args[0]);
            log.info(String.format(">>>>>>>>>>>>转换后的数据为:[%s]",name.toString()));
        }
        if(StringUtils.isEmpty(name.toString())){
            name.append(lockName);
        }

        String requestId = UUID.randomUUID().toString().replace("-","");
        RedisLockEntity lockEntity = new RedisLockEntity(name.toString() ,requestId,timeout,unit);
        try {
            if (type == LockType.LOCK) {
                Boolean lockFlag = distributedLockByRedis.lock(lockEntity);
                log.info("lockEntity:[{}],加锁[{}]",lockEntity,lockFlag);
            }else{
                log.info("aop加锁类型不符,无需进行加锁操作:type:[{}]",type);
            }
            Object object = joinPoint.proceed(joinPoint.getArgs());
            return (R) object;
        } catch (Throwable throwable) {
            log.error("aop处理内部异常：",throwable);
            return new R(OpCode.Internal.getCode(),OpCode.Internal.getValue());
        }finally {
            if (type == LockType.LOCK) {
                Boolean unLockFlag = distributedLockByRedis.unlockLua(lockEntity);
                log.info("lockEntity[{}],解锁[{}]",lockEntity,unLockFlag);
            }
        }
    }
 }   
```



4. 调用含自定义注解的具体方法

com.hongyan.study.distributedlock.controller.RedisController#getAnnotationLock

```
@GetMapping("/annotationLock")
    @DistributeLock(type = LockType.LOCK,name = "data:::annotationLock:::${name}",timeout = 10L)
    public R getAnnotationLock(String name) throws InterruptedException {
        R r = new R(OpCode.sucess.getCode(),OpCode.sucess.getValue(),new RedisLockEntity(redis_key,UUID.randomUUID().toString().replace("-",""),10L,TimeUnit.SECONDS));
//        int i = 1/0;
        Thread.sleep(5000);
        return r;
    }
```





#### 基于Zookeeper的分布式锁

1. 引入所需jar包

```
<!-- zookeeper -->
        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <version>3.4.10</version>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                </exclusion>
                <exclusion>
                    <artifactId>log4j</artifactId>
                    <groupId>log4j</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- curator扩展zookeeper功能-->
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-framework</artifactId>
            <version>2.12.0</version>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-recipes</artifactId>
            <version>2.12.0</version>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```



2. 新增配置文件属性

```
#重试次数
curator.retryCount=5
#重试间隔时间
curator.elapsedTimeMs=5000
# zookeeper 地址
curator.connectString=127.0.0.1:2181
# session超时时间
curator.sessionTimeoutMs=60000
# 连接超时时间
curator.connectionTimeoutMs=5000
```



3. 新增configure配置

```
/**
 * 连接配置
 */
@Configuration
public class CuratorConfiguration {

    @Value("${curator.retryCount}")
    private int retryCount;

    @Value("${curator.elapsedTimeMs}")
    private int elapsedTimeMs;

    @Value("${curator.connectString}")
    private String connectString;

    @Value("${curator.sessionTimeoutMs}")
    private int sessionTimeoutMs;

    @Value("${curator.connectionTimeoutMs}")
    private int connectionTimeoutMs;

    @Bean(initMethod = "start")
    public CuratorFramework curatorFramework() {
        return CuratorFrameworkFactory.newClient(
                connectString,
                sessionTimeoutMs,
                connectionTimeoutMs,
                new RetryNTimes(retryCount, elapsedTimeMs));
    }
}
```



4. 编写zookeeper分布式锁工具类

```
@Service
@Slf4j
public class DistributedLockByCurator implements InitializingBean{

    private final static String ROOT_PATH_LOCK = "rootlock";
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @Autowired
    private CuratorFramework curatorFramework;

    /**
     * 获取分布式锁
     */
    public void acquireDistributedLock(String path) {
        String keyPath = "/" + ROOT_PATH_LOCK + "/" + path;
        while (true) {
            try {
                //创建临时节点
                curatorFramework
                        .create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(keyPath);
                log.info("成功获取锁定路径:{},thread:{}", keyPath,Thread.currentThread().getName());
                break;
            } catch (Exception e) {
                log.info("获取锁定路径失败:{},thread:{}", keyPath,Thread.currentThread().getName());
                log.info("重试中 .......");
                try {
                    if (countDownLatch.getCount() <= 0) {
                        countDownLatch = new CountDownLatch(1);
                    }
                    countDownLatch.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 释放分布式锁
     */
    public boolean releaseDistributedLock(String path) {
        try {
            String keyPath = "/" + ROOT_PATH_LOCK + "/" + path;
            if (curatorFramework.checkExists().forPath(keyPath) != null) {
                //模拟抢占到节点的线程挂掉，后续线程会出现卡死现象
//                if("http-nio-8080-exec-1".equals(Thread.currentThread().getName())){
//                    int i = 1/0;
//                }
                curatorFramework.delete().forPath(keyPath);
            }
            log.info("成功释放锁,thread:{}",Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("释放锁失败,thread:{}",Thread.currentThread().getName());
            return false;
        }
        return true;
    }

    /**
     * 创建 watcher 事件
     * 用于监听事件变更操作：例如：调用释放分布式锁操作时进行节点删除，监听器监听到删除事件发生，则将计数器countDown(将计数器减1)，抢占到资源的新线程则可以正常获取到操作
     * 模拟抢占到节点的线程挂掉后，后续线程会出现卡死现象，但是分布式部署的其他主机确可以监听到该事件，对数据进行解锁操作
     */
    private void addWatcher(String path) throws Exception {
        String keyPath;
        if (path.equals(ROOT_PATH_LOCK)) {
            keyPath = "/" + path;
        } else {
            keyPath = "/" + ROOT_PATH_LOCK + "/" + path;
        }
        final PathChildrenCache cache = new PathChildrenCache(curatorFramework, keyPath, false);
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable().addListener((client, event) -> {
            if (event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                String oldPath = event.getData().getPath();
                log.info("成功释放锁定路径:{},thread:{}", oldPath,Thread.currentThread().getName());
                if (oldPath.contains(path)) {
                    //释放计数器，让当前的请求获取锁
                    countDownLatch.countDown();
                }
            }
        });
    }

    //创建父节点，并创建永久节点
    @Override
    public void afterPropertiesSet() {
        curatorFramework = curatorFramework.usingNamespace("lock-namespace");
        String path = "/" + ROOT_PATH_LOCK;
        try {
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(path);
            }
            addWatcher(ROOT_PATH_LOCK);
            log.info("root path 的 watcher 事件创建成功");
        } catch (Exception e) {
            log.error("connect zookeeper fail，please check the log >> {}", e.getMessage(), e);
        }
    }
}
```



5. 编写具体的调用类controller

```
@RestController
@RequestMapping("/curator")
@Slf4j
public class CuratorController {


    @Autowired
    private DistributedLockByCurator distributedLockByZookeeper;

    private final static String PATH = "test";

    @GetMapping("/lock1")
    public Boolean getLock1() {
        Boolean flag;
        distributedLockByZookeeper.acquireDistributedLock(PATH);
        try {
            log.info("执行lock1操作，更新zk root,thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            flag = distributedLockByZookeeper.releaseDistributedLock(PATH);
        }
        return flag;
    }

    @GetMapping("/lock2")
    public Boolean getLock2() {
        Boolean flag;
        distributedLockByZookeeper.acquireDistributedLock(PATH);
        try {
            log.info("执行lock2操作，更新zk root,thread:{}！！！",Thread.currentThread().getName());
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            flag = distributedLockByZookeeper.releaseDistributedLock(PATH);
        }
        return flag;
    }
}
```



6. 观察分布式锁调用操作及效果









##### 借鉴文档

[分布式锁解决并发的三种实现方式](https://blog.csdn.net/u012867699/article/details/78796114)



