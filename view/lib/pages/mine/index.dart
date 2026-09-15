import 'package:flutter/material.dart';

class MineView extends StatefulWidget {
  const MineView({super.key});

  @override
  _MineViewState createState() => _MineViewState();
}

class _MineViewState extends State<MineView> {
  Widget _buildHeader() {
    return Container(
      // color: Colors.white,
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          CircleAvatar(
            radius: 40,
            backgroundImage: AssetImage('lib/assets/mine/avatar.png'),
          ),
          SizedBox(width: 12),
          Column(
            children: [
              GestureDetector(
                onTap: () {
                  Navigator.pushNamed(context, '/login');
                },
                child: Text(
                  '立即登录',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                ),
              ),
              SizedBox(height: 6),
              Text(''),
            ],
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        // AppBar 右侧按钮区（水平排布）
        actions: [
          // // 右上角「扫一扫」入口
          IconButton(icon: Icon(Icons.qr_code_scanner), onPressed: () {}),
          SizedBox(width: 12),
        ],
      ),
      body: SingleChildScrollView(child: Column(children: [_buildHeader()])),
    );
  }
}
