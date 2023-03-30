## 简介

server-callback-connector 是一个基于 Spring Boot 的融云 IM 聊天机器人服务。它能够接收来自融云的消息回调，并通过调用聊天机器人服务进行响应。该服务可以用于自动回复消息、智能客服等场景。

## 功能

- 接收融云的消息回调，并调用聊天机器人服务进行响应。目前只支持 openai 的文本生成、图片生成、gpt-3.5-turbo 。后续更新计划：
   - 随着百度、华为等国产大模型的开放会跟进支持
   - 随着用户需求的变化会逐步增加相应的功能
- 目前只支持 融云IM 的单聊的 文本消息、引用消息
- 业务流程图：
<img src="https://user-images.githubusercontent.com/41032720/228500506-62b23e64-d112-421d-8124-8a78f58b3bd8.jpg" width="600px">

> 本项目实现了上图标注 1、2 的功能
## 技术栈

- Spring Boot：用于快速搭建基于 Java 的 Web 服务
- JDK 8+：作为 Java 语言的运行环境
- Maven：用于构建和管理项目
- Okhttp：用于发送 HTTP 请求到 AI 模型的 api 和融云 IM 的 api
- Lombok：用于简化 Java 代码的编写
- Caffeine：用于缓存和用户和机器人会话的上下文、会话处理的状态等。如果想要部署多个节点，需要在代理层根据用户id做一致性hash，或者将缓存更换为 redis 等中央缓存
- Thumbnailator：用于将图片下载、压缩、转为base64，融云 IM 的 api 发图片消息接口需要 base64 作为图片的缩略图

## 安装和使用

### 克隆项目

您需要克隆 server-callback-connector 项目到您的本地机器上。您可以使用以下命令将项目克隆到您的本地：

```bash
git clone https://github.com/rongcloud-community/server-callback-connector.git
```

### 注册并创建 OpenAI 的 api key

- 浏览器中打开 https://platform.openai.com ，注册或者登录 OpenAI
- 浏览器中打开 https://platform.openai.com/account/api-keys ，创建 API key 并复制下来（温馨提示：只有创建成功的时候可以复制）

### 配置服务

- 在 融云IM 中创建一个用户作为机器人，创建用户的api文档 https://doc.rongcloud.cn/imserver/server/v1/user/register
- 参考或者直接修改 /src/main/resources/application-local.yml ，文件中有对应的配置说明

### 打包并部署服务

maven 项目的正常打包部署方式都可以，日志默认没有输出到控制台，如果需要输出到控制台需要修改 src/main/resources/logback-spring.xml 文件，将注释掉的控制台输出打开即可

### 配置融云消息回调

- 需要在 融云的开发者后台配置单聊文本消息的消息回调，该消息回调要只处理接受者是机器人的消息
- 消息回调的地址为 {服务域名}/completion/send
- 配置生效后看一下请求是否能够到达 server-callback-connector
- 跟踪一下 server-callback-connector 处理会话是否正常

## 授权

此项目在 [MIT](https://opensource.org/licenses/MIT) 许可下发布，详情请参见 LICENSE.md 文件。

## 贡献者

- [@xiaoliang-wang](https://github.com/xiaoliang-wang) - 项目贡献者

## 致谢

感谢您使用 server-callback-connector ！如果您对此项目有任何疑问或建议，请随时在 GitHub 上提交问题或请求。
