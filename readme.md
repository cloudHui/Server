rote http服务获取gate 地址 限流每分钟1000 同一设备号一秒一次
gate tcp服务根据消息id 转发所有的消息  就保留玩家id clientId 和hallId  roomId  gameId
hall tcp服务查询签到活动商店等信息玩家属性数据
game tcp服务服务游戏玩法


客户端所有请求失败重复三次请求