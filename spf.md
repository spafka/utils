# 榆林化学keycloak 配置说明



###### 登入账户

打开 ip:port/auth如图所示

![image-20210624134713957](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624134713957.png)



点击admin console 登入管理员账户

![image-20210624134817152](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624134817152.png)

默认账号 密码 admin/admin

#### keycloak配置

![image-20210624134906182](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624134906182.png)



##### keycloak配置界面如图

最左上角显示租户信息，选择切换至dt租户

![image-20210624135719940](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624135719940.png)

configure:  配置

Realm Setting: 租户设置

client: 客户端设置

client scope: 客户端的权限设置

1. 登入主图设置 点击Realm Setting

![image-20210624140501686](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624140501686.png)

选择主题设置 设置login theme 为supos-theme

国际化Internationalization Enabled

默认语言选 Default Locale

2.  client scpoe设置

   点击新建client scope

   ![image-20210624140839445](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624140839445.png)

   输入Name ingr.api，save保存

   ![image-20210624140958961](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624140958961.png)

   此时 client有

 ![image-20210624141112235](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624141112235.png)



点击edit 编辑

![image-20210624141221687](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624141221687.png)

为client scope 添加 token 映射

claim 为 token里的kv映射,name 为token的key，vaule为认证对象的属性，如name，email等。Mapper type为从认证对象获取属性value的方法。

![image-20210624141320113](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624141320113.png)

![image-20210624141442609](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624141442609.png)

![image-20210624141542184](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624141542184.png)



依次添加如下mapper

| Name            | Type             | Claim NAME       | ClAIM valUe                                  | Claim JSON Type |
| --------------- | ---------------- | ---------------- | -------------------------------------------- | --------------- |
| ClientHostName  | Hardcoded claim  | ClientHostName   | 192.168.50.37                                | String          |
| amr             | Hardcoded claim  | amr              | password                                     | String          |
| scp             | Hardcoded claim  | scp              | [ "openid", "email", "profile", "ingr.api" ] | JSON            |
| name            | User's full name | name             |                                              |                 |
| role            | Hardcoded claim  | role             | system_admin                                 | String          |
| idp             | Hardcoded claim  | idp              | Ping Federate                                | String          |
| scope           | Hardcoded claim  | scope            | [ "openid", "email", "profile", "ingr.api" ] | JSON            |
| aud             | Hardcoded claim  | aud              | EE9C5479-A52E-4D11-80AE-BFDDCE9A603F         | String          |
| ingr.session_id | User Session     | ingr\.session_id |                                              | String          |
| client_id       | Hardcoded claim  | client_id        | SMY02WC                                      | String          |

![image-20210624143856131](C:\Users\supos\AppData\Roaming\Typora\typora-user-images\image-20210624143856131.png)





