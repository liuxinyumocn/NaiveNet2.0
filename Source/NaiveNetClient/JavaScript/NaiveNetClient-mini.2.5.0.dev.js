/**
    NaiveNet
    NaiveNetClient-MiniProgame.2.5.+.js
    Author: Xinyu Liu
*/

class Format {

    callback(param) {
        param.success = param.success || function () { };
        param.fail = param.fail || function () { };
        param.complete = param.complete || function () { };
        param._success = function (res) {
            param.success(res);
            param.complete(res);
        };
        param._fail = function (res) {
            param.fail(res);
            param.complete(res);
        };
    }

}

const format = new Format();

const codemap = {

    200 : 'ok',
    400 : 'not found controller',
    401 : 'not found channel',
    402 : 'cannot be established',
    403 : 'channel refuse connect',
    404 : 'channel not established with server',
    500 : 'permission denied',
    501 : 'data format error',
    502 : 'unknow error',
    503 : 'recover failed',

}

//evn
let handler = null;
if("undefined" != typeof wx){ //微信小程序环境
    handler = wx;
}else if("undefined" != typeof tt){ //字节跳动小程序环境
    handler = tt;
}
function outdata(data){
    if(handler){
        return {data:data};
    }else{
        return data;
    }
}

class NaiveModule{

    constructor(){

        this._ctrls= {};
    }

    addController(name,func = function(param,response = function(){}){}){
        this._ctrls[name] = func;
    }

    removeController(name){
        if(this._ctrls[name])
            this._ctrls[name] = null;
    }

    _do(opt,callback = function(){}){
        if(this._ctrls[opt.controller]){
            this._ctrls[opt.controller](opt,callback);
            return true;
        }else{
            return false;
        }
    }

}

export default class NaiveNet {

    constructor(autocode = true){
        this._sockettask = null;
        this._level = 0;    // 1 是连接状态 0 是断开状态
        this._autocode = autocode;

        //临时ID资源管理
        this._t_id = 0;
        this._t_msg = {};   //已发出消息
        this._t_wt = [];    //等待中消息

        //回执请求超时
        this._timeout = 5000;   //超过5000秒未收到回执则请求超时

        //ping值
        this._ping = 0;
        this._hearttimeout = 10000; //心跳 默认每隔10秒心跳一次

        this._auth = false;
        this._sessionid = '';
        this._ns = "";

        //频道信息
        this._channel = {}
        this._channel_cache = {}

        //boxs
        this._boxs = [];

        //几组基本事件
        this.onBreak = function(){};
        this.onRecover = function(){};
        this.onAuth = function(){}
        this.onPingChange = function(){}

        this._donotcallOnbreak =false;

    }

    /**
        设置SESSION TOKEN
    */
    setToken(token){
        this._sessionid = token;
    }

    /*
        获取本机SESSION TOKEN
    */
    getToken(){
        if(this._auth)
            return this._sessionid;
        return null;
    }


    /**
        与NS服务器建立连接
        opt = {
            'ns' => ' ', NS服务器地址需要携带协议头 例如 wss://naivenet.cn <p>
            success , fail , complete
        }
    */
    connect(opt){
        opt = opt || {};
        format.callback(opt);
        this.close();

        if(handler){
            this._sockettask = handler.connectSocket({
                url:  opt.ns,
                success:(res)=>{

                },
                fail:(res)=>{
                    opt._fail({
                        code:400,
                        errmsg:'无法建立网络连接'
                    })
                }
            })

            this._sockettask.onOpen(function(){
                this._level = 1;
                this._ns = opt.ns;
                opt._success({
                    code:200,
                    errmsg:'成功建立连接'
                })
                this._autoheart();
                return;
            }.bind(this));
            this._sockettask.onClose( function(res){
                this._level = 0;
                this._onBreak();
            }.bind(this));
            this._sockettask.onMessage( function(res){
                this._parseMsg(res)
            }.bind(this))
            this._sockettask.onError(function(res){
                this.level = 0;
                opt._fail({
                    code:400,
                    errmsg:'网络连接失败'
                })
            }.bind(this))

        }else{
            this._sockettask = new WebSocket(opt.ns);
            this._sockettask.binaryType = "arraybuffer";
            if(!this._sockettask){
                opt._fail({
                    code:400,
                    errmsg:'连接失败'
                })
                return;
            }
            this._sockettask.onopen = function(){
                this._level = 1;
                this._ns = opt.ns;
                opt._success({
                    code:200,
                    errmsg:'成功建立连接'
                })
                this._autoheart();
                return;
            }.bind(this);
            this._sockettask.onclose = function(res){
                this._level = 0;
                this._onBreak();
            }.bind(this);
            this._sockettask.onmessage = function(res){
                this._parseMsg(res)
    
            }.bind(this);
            this._sockettask.onerror = function(res){
    
            }.bind(this);
        }
    }

    /*
        主动关闭连接（不可恢复 服务器资源将立即被释放）
    */
    close(opt){
        opt = opt || {};
        format.callback(opt);
        if(this._sockettask){
            this.request({
                controller:'close',
                complete:(res)=>{
                    this._sockettask.close();
                    this._level = 0;
                    opt._success({
                        code:200
                    })
                }
            })
        }else{
            opt._success({
                code:200
            })
        }

    }

    /*
        主动临时断线（可恢复）
        opt = {
            eventstatus    是否想产生onbreak的事件回调 不填写则代表产生回调 否则 不产生
        }
    */
    break(opt){
        opt = opt || {};
        format.callback(opt);
        if(this._level == 1){
            this._level = 0;

            if(opt.eventstatus === undefined)
                opt.eventstatus = true;

            if(!opt.eventstatus)
                this._donotcallOnbreak = true;
            else
                this._donotcallOnbreak = false;
            this._sockettask.close();
        }
    }

    /*
        尝试恢复连接
        opt = {
            ns:'', 可空，如果留空则使用内存中ns地址，注意：若使用内存ns地址，无法恢复程序重启后的断线恢复
            token: '' 可空，如果留空则使用内存中token，注意：若使用内存token，无法恢复程序重启后的断线恢复,
            success,fail,complete
        }
    */
    recover(opt){
        opt = opt || {};
        format.callback(opt);
        opt.token = opt.token || this._sessionid;
        opt.ns = opt.ns || this._ns;
        if(this._level == 1){
            opt._success({
                code:200,
                errmsg:'ok'
            });
            return;
        }

        //开始恢复网络
        this.connect({
            ns:opt.ns,
            success:(res)=>{
                //开始尝试恢复
                this.request({
                    channel:'',
                    controller:'recover',
                    data:opt.token,
                    success:(res)=>{
                        this._level = 1;
                        opt._success(res);
                        this.onRecover();
                    },
                    fail:(res)=>{
                        this.break({
                            eventstatus:false
                        })
                        opt._fail(res);
                    }
                })

            },
            fail:(res)=>{
                opt._fail(res);
            }

        })

    }

    /*
        进入频道连接 与频道交互前必须先与频道建立连接，通常情况下不需要开发者进行主动连接，NaiveNet会自动在必要的时候进行连接
        opt = {
            channel:'',
            cache:true, //是否使用缓存状态 使用缓存状态可减少一次查询请求
            success:
            fail
            complete
        }
    */
    enterChannel(opt){
        opt = opt || {};
        format.callback(opt);

        if(opt.channel == '')
        {
            opt._success({
                code:200,
                errmsg:'ok connect ns'
            })
            return;
        }

        let key = this._ns + "_" + opt.channel;
        if(opt.cache){
            let status = this._channel_cache[key];
            if(status){
                opt._success({
                    code:200,
                    errmsg:'ok use cache'
                })
                return;
            }
        }
        this.request({
            channel:'',
            controller:'enterChannel',
            data:opt.channel,
            success:(res)=>{
                //频道连接成功
                this._channel_cache[key] = true;
                opt._success({
                    code:200,
                    errmsg:'ok no cache'
                })
            },
            fail:(res)=>{
                opt._fail(res);
            }
        })


    }

    //释放频道资源（但NS连接仍然保持）
    quitChannel(opt){
        opt = opt || {};
        format.callback(opt);
        this.getChannelID({
            channel:opt.channel,
            success:(res)=>{
                this.request({
                    channel:'',
                    controller:'quitChannel',
                    data:res.id+'',
                    complete:(res)=>{
                        opt._success({
                            code:200,
                            errmsg:'ok'
                        })
                    }
                })
            }
        })
    }

    //提供频道别名 查询 ID
    // opt = {channel:'',success:}   res.id 为对应ID
    getChannelID(opt){
        opt = opt || {};
        format.callback(opt);
        opt.channel = opt.channel || '';
        if(opt.channel == ''){
            opt._success({
                code:200,
                id:0
            })
            return;
        }
        let database = this._channel[this._ns];
        if(!database){ //不存在该库
            this._channel[this._ns] = {};
            database = {};
        }
        //查询该库中是否存在缓存
        let channel = database[opt.channel];
        if(channel){ //存在
            opt._success({
                code:200,
                id:channel
            })
            return;
        }
        //不存在 去拉取
        this.request({
            channel:'',
            controller:'getChannel',
            data:opt.channel,
            success:(res)=>{
                if(res.code == 200){
                    let id = parseInt(res.data);
                    this._channel[this._ns][opt.channel] = id;
                    opt._success({
                        code:200,
                        id: id
                    })
                    return;
                }
                opt._fail({
                    code:400,
                    errmsg:'获取失败'
                })
            },
            fail:(res)=>{
                opt._fail(res);
            }
        })
    }

    /**
        opt={
            channel:'', //如果留空则代表向NS发出请求
            controller:'',
            data:[], 可以是数值 、 字符串 也可以是 Array Buffer
            success,fail,complete
        }

    */
    request(opt){
        opt = opt || {};
        format.callback(opt);
        opt.channel = opt.channel || "";
        if(opt.data === undefined)
            opt.data = "";
        else{
            if (typeof opt.data == 'number'){
                opt.data = ""+opt.data;
            }
        }
        if(!opt.controller){
            opt._fail({
                code:400,
                errmsg:'控制器（controller）不能为空'
            });
            return;
        }
        if(this._level != 1){
            opt._fail({
                code:400,
                errmsg:'当前状态无法发出请求'
            });
            return;
        }

        this.getChannelID({
            channel:opt.channel,
            success:(res2)=>{
                this.enterChannel({
                    channel:opt.channel,
                    cache:true,
                    success:(res)=>{
                        let second = false;
                        let _opt = {
                            channelid:res2.id,
                            data:opt.data,
                            controller:opt.controller,
                            success:(res)=>{
                                opt._success(res);
                            },
                            fail:(res)=>{
                                if(res.code == 404){ //说明还没有与NS建立连接
                                    if(second){
                                        opt._fail(res);
                                        return;
                                    }
                                    second = true;
                                    this.enterChannel({
                                        channel:opt.channel,
                                        cache:false,
                                        success:(res)=>{
                                            this._request(_opt);
                                        },
                                        fail:(res)=>{
                                            opt._fail(res);
                                        }
                                    })
                                }else{
                                    opt._fail(res);
                                }
                            }
                        }

                        this._request(_opt);
                    },
                    fail:(res)=>{
                        opt._fail(res);
                    }
                })
            },
            fail:(res)=>{
                opt._fail(res);
            }
        })

    }

    createBox(){
        return new NaiveModule();
    }

    /**
     * 添加Box
    */
    addBox(mod){
        for(let i = 0 ; i < this._boxs.length;i++){
            if(this._boxs[i] == mod)
                return;
        }
        this._boxs.push(mod);
    }

    removeBox(mod){
        for(let i = 0 ;i < this._boxs.length;i++){
            if(this._boxs[i] == mod){
                for(let j = i+1;j<this._boxs.length;j++){
                    this._boxs[j-1] = this._boxs[j];
                }
                this._boxs.pop();
                return true;
            }
        }
        return false;
    }

    
    _genTempMsgId(){
        for(let i = 0; i < 125 ;i++){
            
            if(this._t_id > 125)
                this._t_id = 0;

            let item = this._t_msg[""+this._t_id];
            if(!item){
                return this._t_id++;
            }

            this._t_id++;

        }
        return -1;
    }

    _request(opt,channelid){
        opt = opt || {};
        format.callback(opt);
        let channelID = opt.channelid;
        //分配临时ID 0~255
        
        let id = this._genTempMsgId();
        if(id == -1){
            this._t_wt.push(opt);
            return;
        }
        //资源未满
        let record = [
            opt,
            false,       //未处理
            -1,         //定时器句柄
            new Date().getTime(),   //发送时的时间戳
        ];
        this._t_msg[''+id] = record;

        let data = opt.data || "";
        if (typeof data == 'string') {
            //将字符串转换为ArrayBuffer
            data = this._stringToArrayBuffer(data);
        }

        //设置channelID信息
        let channelIDData = this._genLenArrayBuffer(channelID);
        //设置controller信息
        let controllerData = this._stringToArrayBuffer(opt.controller);
        let controllerDataU8 = new Uint8Array(controllerData);
        let controllerHeader = this._genLenArrayBuffer(controllerDataU8.length);
        //设置正文信息
        let contentData = this._stringToArrayBuffer(opt.data);
        let contentDataU8 = new Uint8Array(contentData);
        let contentDataHeader = this._genLenArrayBuffer(contentDataU8.length);

        //组合最终的输出流
        let _dataA = new ArrayBuffer(channelIDData.length + controllerDataU8.length + controllerHeader.length + contentDataU8.length + contentDataHeader.length + 2);
        let _data = new Uint8Array(_dataA);
        _data[0] = 1;
        _data[1] = id;      //临时ID
        _data.set(channelIDData,2);
        let index = 2 + channelIDData.length;
        _data.set(controllerHeader,index);
        index += controllerHeader.length;
        _data.set(controllerDataU8,index);
        index += controllerDataU8.length;
        _data.set(contentDataHeader,index);
        index += contentDataHeader.length;
        _data.set(contentDataU8,index);
        //完成 开始发送
        try{
            this._sockettask.send(outdata(_data.buffer));
            //成功发送 等待确认回执
            this._t_msg[''+id][2] = setTimeout(function(){
                //超时未得到回应
                opt._fail({
                    code:401,
                    errmsg:'响应超时'
                });
                //释放留样
                //this._t_msg[''+id][1] = true;
                delete this._t_msg[''+id]
                //检查等待资源
                this._sendCheckWt();
            }.bind(this),this._timeout);
        }catch(e){
            opt._fail({
                code: 400,
                errmsg: '发送失败'
            });
            //释放留样
            //this._t_msg[''+id][1] = true;
            delete this._t_msg[''+id]
            this._sendCheckWt();
        }
    }

    /*
        心跳检测
    */
    _autoheart(){
        //自动心跳
        let next = function(){
            if(this._level == 1){
                this.request({
                    controller:'heart',
                    data: ''+this._ping
                });
                setTimeout(next.bind(this),this._hearttimeout);
            }
        }
        next.bind(this)();
    }

    //当发生连接中断时
    _onBreak(){
        if(!this._donotcallOnbreak)
            this.onBreak({
                code:400,
                errmsg:'break'
            });
        this._donotcallOnbreak = false;
    }

    _parseMsg(res){
        let udata = new Uint8Array(res.data);
        if(udata[0] == 0 || udata[0] == 3){ //0 代表正常回复 3 代表异常回复
            //在回复某一请求的响应
            let id = udata[1];
            let msg = this._t_msg[''+id];
            //判断是否有必要处理该消息
            if(!msg)  //该消息已无需处理
                return;
            //解析频道ID
            let index = 2;
            let channel_data = this._parseLenArrayBuffer(index,udata);
            let channel_id = channel_data[1];

            //解析参数长度
            index += channel_data[0];
            let param_len = this._parseLenArrayBuffer(index,udata);
            index += param_len[0];
            let param_content = this._parseContentArrayBuffer(index,udata,param_len[1]);

            let ping = new Date().getTime() - msg[3];
            this._setPing(ping);
            //msg[1] = true;
            clearTimeout(msg[2]);
            delete  this._t_msg[''+id];

            if(udata[0] == 0){ //正常回复
                msg[0]._success({
                    code : 200,
                    originData : param_content,
                    data : this._autocode ? this.Uint8ArrayToString(param_content) : null,
                    errmsg : codemap[200]
                })
            }else{ //异常回复
                //ole.log('异常回复',udata);
                let code = parseInt(this.Uint8ArrayToString(param_content))
                msg[0]._fail({
                    code : code,
                    errmsg: codemap[code]
                })
            }
            this._sendCheckWt();
            return;
        }else if(udata[0] == 1){ //请求
            let control = udata[0];
            let msgid = udata[1];
            //解析频道
            let index = 2;
            let channelid_data = this._parseLenArrayBuffer(index,udata);
            let channelid = channelid_data[1];
            index += channelid_data[0];
            let end = index - 1;
            let ctrl_len = this._parseLenArrayBuffer(index,udata);
            index += ctrl_len[0];
            let ctrl_content = this._parseContentArrayBuffer(index,udata,ctrl_len[1]);
            index += ctrl_len[1];
            let param_len = this._parseLenArrayBuffer(index,udata);
            index += param_len[0];
            let param_content = this._parseContentArrayBuffer(index,udata,param_len[1]);
            if(channelid == 0){ //来自NS的请求
                this._dealNSToClient(
                    this.Uint8ArrayToString(ctrl_content),
                    this.Uint8ArrayToString(param_content)
                )
            }else{ //来自NC的请求
                this._dealNCToClient(
                    {
                        originContent:udata,
                        originHeaderEndIndex:end,
                        originMsgID:msgid,
                        originChannelID:channelid,
                        controller:this.Uint8ArrayToString(ctrl_content),
                        dataBytes:param_content,
                        data: this._autocode ? this.Uint8ArrayToString(param_content) : null
                    }
                )
            }

        }

    }

    //处理来自NS服务器的请求
    _dealNSToClient(controller,param){

        switch(controller){
            case 'auth':
                this._auth = true;
                this._sessionid = param;
                this.onAuth({
                    code:200,
                    errmsg:'ok',
                    token:param
                })
                break;

        }

    }

    //处理来自频道的请求
    /*
        {
            originContent:udata,
            originHeaderEndIndex:end,
            originMsgID:msgid,
            originChannelID:channelid,
            controller:this.Uint8ArrayToString(ctrl_content),
            dataBytes:param_content,
            data: this._autocode ? this.Uint8ArrayToString(param_content) : null
        }
    */
    _dealNCToClient(opt){

        let t = this;
        let response = function(res = ''){
            t._response(opt,true,res)
        }
        for(let i in this._boxs){
            let r = this._boxs[i]._do(opt,response);
            if(r){ //找到对应控制器
                return;
            }
        }
        //未找到对应控制器
        this._response(opt,false,null);

    }

    //回复
    /*
        {
            originContent:udata,
            originHeaderEndIndex:end,
            originMsgID:msgid,
            originChannelID:channelid,
            controller:this.Uint8ArrayToString(ctrl_content),
            dataBytes:param_content,
            data: this._autocode ? this.Uint8ArrayToString(param_content) : null
        }
    */
    _response(opt,success,param = null){
        let data = (typeof param == 'string') ? this._stringToArrayBuffer(param) : param;
        let data_U8 = new Uint8Array(data);
        let data_len = this._genLenArrayBuffer(data_U8.length);

        let content = new ArrayBuffer(opt.originHeaderEndIndex + 1 + data_len.length + data_U8.length);
        let _content = new Uint8Array(content);

        for(let i = 0; i<= opt.originHeaderEndIndex ;i++){
            _content[i] = opt.originContent[i];
        }
        if(success){
            _content[0] = 0;
        }else{
            _content[0] = 3;
        }
        let index = opt.originHeaderEndIndex + 1;
        _content.set(data_len,index);
        index += data_len.length;
        _content.set(data_U8,index);

        //onsole.log(_content.buffer)
        this._sockettask.send(
            outdata(_content.buffer)
        );
    }

    /*
        检查等待资源
    */
    _sendCheckWt(){
        if(this._t_wt.length > 0){
            let item = this._t_wt.shift();
            this._request(item);
        }
    }

    _setPing(ping){
        this._ping = parseInt(ping/2);
        this.onPingChange(this._ping);
    }

    getPing(){
        return this._ping;
    }

    //解析长度位算法
    _parseLenArrayBuffer(start,data){
        let value = 0;
        let n = 0;
        for(let i = start;i<data.length;i++){
            let a = data[i];
            value += a;
            n++;
            if(a != 255){
                break;
            }
        }
        return [n,value];
    }
    
    //解析长度位正文
    _parseContentArrayBuffer(start,data,length){
        //其中大于255 一个数字占3位
        let _data = [];
        let index = start;
        for(let i = 0; i< length;index++){
            _data.push(data[index]);
            i+=1;
        }
        let content = new Uint8Array(_data);
        return content;
    }

    //生产长度位算法
    _genLenArrayBuffer(length){
        let len = parseInt(length / 255 ) + 1;
        let data = new ArrayBuffer(len);
        let _data = new Uint8Array(data);
        for(let i = 0;i<len;i++){
            if(length > 255){
                _data[i] = 255;
                length -= 255;
            }else{
                _data[i] = length;
                break;
            }
        }
        return _data;
    }

    _stringToArrayBuffer(str) {
        var bytes = new Array(); 
        var len,c;
        len = str.length;
        for(var i = 0; i < len; i++){
            c = str.charCodeAt(i);
            if(c >= 0x010000 && c <= 0x10FFFF){
                bytes.push(((c >> 18) & 0x07) | 0xF0);
                bytes.push(((c >> 12) & 0x3F) | 0x80);
                bytes.push(((c >> 6) & 0x3F) | 0x80);
                bytes.push((c & 0x3F) | 0x80);
            }else if(c >= 0x000800 && c <= 0x00FFFF){
                bytes.push(((c >> 12) & 0x0F) | 0xE0);
                bytes.push(((c >> 6) & 0x3F) | 0x80);
                bytes.push((c & 0x3F) | 0x80);
            }else if(c >= 0x000080 && c <= 0x0007FF){
                bytes.push(((c >> 6) & 0x1F) | 0xC0);
                bytes.push((c & 0x3F) | 0x80);
            }else{
                bytes.push(c & 0xFF);
            }
      }
      var array = new Int8Array(bytes.length);
      for(var i in bytes){
        array[i] =bytes[i];
      }
        return array.buffer;
    }

    _arrayBufferToString(arr){
        if(typeof arr === 'string') {  
            return arr;  
        }  
        var dataview=new DataView(arr.data);
        var ints=new Uint8Array(arr.data.byteLength);
        for(var i=0;i<ints.length;i++){
          ints[i]=dataview.getUint8(i);
        }
        arr=ints;
        var str = '',  
            _arr = arr;  
        for(var i = 0; i < _arr.length; i++) {  
            var one = _arr[i].toString(2),  
                v = one.match(/^1+?(?=0)/);  
            if(v && one.length == 8) {  
                var bytesLength = v[0].length;  
                var store = _arr[i].toString(2).slice(7 - bytesLength);  
                for(var st = 1; st < bytesLength; st++) {  
                    store += _arr[st + i].toString(2).slice(2);  
                }  
                str += String.fromCharCode(parseInt(store, 2));  
                i += bytesLength - 1;  
            } else {  
                str += String.fromCharCode(_arr[i]);  
            }  
        }  
        return str; 
    }

    
    Uint8ArrayToString(array){
        var out, i, len, c;
        var char2, char3;
     
        out = "";
        len = array.length;
        i = 0;
        while(i < len) {
        c = array[i++];
        switch(c >> 4)
        { 
          case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
            // 0xxxxxxx
            out += String.fromCharCode(c);
            break;
          case 12: case 13:
            // 110x xxxx   10xx xxxx
            char2 = array[i++];
            out += String.fromCharCode(((c & 0x1F) << 6) | (char2 & 0x3F));
            break;
          case 14:
            // 1110 xxxx  10xx xxxx  10xx xxxx
            char2 = array[i++];
            char3 = array[i++];
            out += String.fromCharCode(((c & 0x0F) << 12) |
                           ((char2 & 0x3F) << 6) |
                           ((char3 & 0x3F) << 0));
            break;
        }
        }
     
        return out;
      }
}
