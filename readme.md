center 所有服务的注册中心 维持和各个服务的心跳 gate 连接以后获取 其他所有服务的地址并链接 链接更新的时候同步其他服务
gate tcp服务根据消息id 转发所有的消息  就保留玩家id clientId 和hallId  roomId  gameId 主动连center 获取其他服务的端口和ip 然后去主动链接
hall tcp服务查询签到活动商店等信息玩家属性数据 主动注册center 提供ip和端口
game tcp服务服务游戏玩法 主动注册center 提供ip和端口
