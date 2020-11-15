// page/index/index.js
import NaiveNet from '../../lib/NaiveNetClient-mini.2.0.0.dev';
let ns = new NaiveNet();

ns.onBreak = function(){
    console.log('发生断线');
}

let box = ns.createBox();
box.addController(
    "test",
    function(res,response){
        
        console.log(res);

        response('hello!');
    }
)
ns.addBox(box);


Page({

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {

    },

    conn: function(){
        ns.connect({
            ns:"ws://127.0.0.1:4000",       // wss://xxxxxxx
            success:(res)=>{
                console.log(res);
            },
            fail:(res)=>{
                console.log("失败",res);
            }
        })
    },
    auth:function(){

        ns.request({
            channel:'my_channel_1',
            controller:'auth',
            data:'123456asd',
            success:(res)=>{
                console.log(res);
            },
            fail:(res)=>{
                console.log("Failed:",res);
            }

        })

    },
    request:function(){

        ns.request({
            channel:'my_channel_1',
            controller:'t',
            data:'',
            success:(res)=>{
                console.log(res);
            }

        })

    },
    break:function(){
        ns.break();
    },
    recover:function(){
        ns.recover({
            success:(res)=>{
                console.log(res);
            },
            fail:(res)=>{
                console.log(res);
            }
        })
    },
    quitChannel:function(){

    },
    close:function(){
        ns.close();
    }


})