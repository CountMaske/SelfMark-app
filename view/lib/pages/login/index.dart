import 'package:flutter/material.dart';

import 'dart:ui';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  _LoginPageState createState() => _LoginPageState();
}
// SingleTickerProviderStateMixin：为 TabController 提供 Ticker（动画驱动），
//   因为 TabController 的 vsync 需要一个 TickerProvider。
// with 把多个 mixin 的功能混入一个类中，实现代码复用。
//   如果一个类拥有某个接口要求的全部成员，它就可以被当作那个接口使用。
// vsync 需要 TickerProvider，而 SingleTickerProviderStateMixin 实现了这个接口；
//   with 之后，_LoginPageState 获得了这个实现，因而也成为 TickerProvider，
//   所以能把 this 传给 vsync。
class _LoginPageState extends State<LoginPage>
    with SingleTickerProviderStateMixin {
  // Tab 控制器，管理"登录/注册"两个标签页的切换
  // late告诉编译器：这个变量我暂时不初始化，但保证在第一次使用前一定会赋值，你不用报错。
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    // 创建 TabController：2 个
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    // 释放控制器，避免内存泄漏
    _tabController.dispose();
    super.dispose();
  }

  Padding _buildLoginTab() {
    return Padding(
      padding: EdgeInsets.all(20),
      child: Column(
        children: [
          _buildTextField('手机号'),
          SizedBox(height: 20),
          _buildTextField('密码'),
          SizedBox(height: 30),
          _buildGrandientButton('登录'),
        ],
      ),
    );
  }

  Padding _buildRegisterTab() {
    return Padding(
      padding: EdgeInsets.all(20),
      child: Column(
        children: [
          _buildTextField('手机号'),
          SizedBox(height: 20),
          _buildTextField('密码'),
          SizedBox(height: 30),
          _buildGrandientButton('注册'),
        ],
      ), // Column
    ); // Padding
  }

  TextField _buildTextField(String text) {
    // 通用输入框：白色文字 + 白色下划线边框
    return TextField(
      style: TextStyle(color: Colors.white), // 输入文字颜色
      cursorColor: Colors.white, // 光标颜色
      decoration: InputDecoration(
        hintText: text, // 占位提示
        hintStyle: TextStyle(color: Colors.white70), // 提示文字颜色 
        // 未聚焦时的下划线边框      
        enabledBorder: UnderlineInputBorder(
          borderSide: BorderSide(color: Colors.white70),
        ),
         // 聚焦时的下划线边框
        focusedBorder: UnderlineInputBorder(
          borderSide: BorderSide(color: Colors.white70),
        ),
      ),
    );
  }

  // 渐变按钮：圆角 + 蓝紫渐变背景
  Container _buildGrandientButton(String text) {
    return Container(
      width: double.infinity, // 占满可用宽度
      height: 50, // 按钮高度
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(25), // 圆角胶囊形
        gradient: LinearGradient(colors: [Colors.blue, Colors.indigoAccent]), // 渐变配色
      ),
      child: Center(
        child: Text(
          text,
          style: TextStyle(
            color: Colors.white, // 按钮文字白色
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        // Stack 层级（从下往上）：
        //   1. 背景图
        //   2. 黑色半透明遮罩
        //   3. 居中毛玻璃卡片
        children: [
          Container(
            decoration: BoxDecoration(
              image: DecorationImage(
                image: AssetImage('lib/assets/mine/login_background.jpg'),
                fit: BoxFit.cover, // 铺满且不变形
              ),
            ),
          ),
          // 压暗背景图，让上方文字更清晰
          Container(color: Colors.black.withValues(alpha: 0.3)),
          Center(
            child: ClipRRect(
              // 裁剪圆角，让 BackdropFilter 的模糊也被裁进圆角内
              borderRadius: BorderRadius.circular(20),
              child: BackdropFilter(
                // 毛玻璃模糊滤镜
                filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                child: Container(
                  height: 400,  // 卡片高度
                  width: 300, // 卡片宽度
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2), // 半透明白底
                    borderRadius: BorderRadius.circular(20), // 圆角
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.6), // 边框
                      width: 1,
                    ),
                  ),
                  child: Column(
                    children: [
                      // 顶部 Tab 栏：登录 / 注册
                      TabBar(
                        // 绑定控制器：TabBar 从它读取当前选中项、切换动画进度
                        // 用户点击标签时，也会通过它切换索引
                        controller: _tabController,
                        // 指示器（选中高亮块）的宽度策略：
                        // TabBarIndicatorSize.tab = 铺满整个 tab 宽度
                        // 另一个可选值 .label 表示只包住文字
                        indicatorSize: TabBarIndicatorSize.tab,
                        // 自定义指示器外观：这里用蓝→靛蓝的渐变背景块
                        // 不写的话默认是一条下划线
                        indicator: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [Colors.blue, Colors.indigoAccent],
                          ),
                        ),
                        // 标签列表：数量要和 TabController 的 length 一致（这里是 2）
                        tabs: [
                          Tab(text: '登录'),
                          Tab(text: '注册'),
                        ],
                        labelColor: Colors.white, // 选中标签的文字颜色
                        unselectedLabelColor: Colors.white70, // 未选中标签的文字颜色（半透明白）
                      ),
                      // 下方内容区：随 Tab 切换显示对应表单
                      // TabBarView 只负责"内容页"这部分 UI
                      // 它和上面的 TabBar 共用同一个 controller，所以能双向同步：
                      //   - 点标签 → 内容跟着切
                      //   - 左右滑内容 → 标签跟着切
                      Expanded(
                        // Expanded 让内容区占满 Column 剩余的全部高度
                        // （Column 里必须给 TabBarView 一个确定高度，否则会溢出/报错）
                        child: TabBarView(
                          controller: _tabController, // 与 TabBar 共用，保证同步
                          children: [_buildLoginTab(), _buildRegisterTab()], // 第 0、1 个 tab 对应的内容
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
