// 基于dio进行二次封装
import 'package:dio/dio.dart';
import 'package:view/contants/index.dart';
import 'package:view/stores/TokenManager.dart';

class DioRequest {
  // dio请求对象
  final Dio _dio = Dio();
  // 基础地址拦截器
  DioRequest() {
    _dio.options
      ..baseUrl = GlobalConstants.BASE_URL
      ..connectTimeout = Duration(seconds: GlobalConstants.TIME_OUT)
      ..sendTimeout = Duration(seconds: GlobalConstants.TIME_OUT)
      ..receiveTimeout = Duration(seconds: GlobalConstants.TIME_OUT);
    // 拦截器
    _addInterceptor();
  }

  // 添加拦截器
  void _addInterceptor() {
    // 添加请求拦截器
    _dio.interceptors.add(
      // 拦截器包装类。它本质上是 Interceptor 的一个便捷实现
      InterceptorsWrapper(
        onRequest: (request, handler) {
          // 注入token：只追加Authorization，不覆盖其他header（如Content-Type）
          if (tokenManager.getToken().isNotEmpty) {
            request.headers["Authorization"] = "Bearer ${tokenManager.getToken()}";
          }
          handler.next(request); // 放行
        },
        onResponse: (response, handler) {
          // http状态码 200-299 放行，否则拒绝（把response带给onError以便提取msg）
          if (response.statusCode! >= 200 && response.statusCode! < 300) {
            handler.next(response);
            return;
          }
          // 非 2xx 手动转成 DioException，
          // 这样会进入 onError，方便统一提取 msg
          // handler.reject把这个请求标记为失败，并抛出一个 DioException 错误，抛给上层 try/catch 或下一个拦截器的 onError 去处理。
          handler.reject(
            DioException(
              // 保留原始请求信息，方便上层定位是哪个请求出错
              requestOptions: response.requestOptions,
              // 带上响应体，上层才能取到后端的 msg、code 等数据
              response: response,
            ),
          );
        },
        onError: (error, handler) {
          // 后端异常统一返回 {code, msg, data} 结构，取出msg用于提示
          handler.reject(
            DioException(
              requestOptions: error.requestOptions,
              message: error.response?.data?["msg"] ?? "网络异常",
            ),
          );
        },
      ),
    );
  }

  Future<dynamic> get(String url, {Map<String, dynamic>? params}) {
    return _handleResponse(_dio.get(url, queryParameters: params));
  }

  // 定义post接口
  Future<dynamic> post(String url, {Map<String, dynamic>? data}) {
    return _handleResponse(_dio.post(url, data: data));
  }

  // 进一步处理返回结果的函数
  Future<dynamic> _handleResponse(Future<Response<dynamic>> task) async {
    try {
      Response<dynamic> res = await task;
      final data = res.data as Map<String, dynamic>; // data才是我们真实的接口返回的数据
      if (data["code"] == GlobalConstants.SUCCESS_CODE) {
        // 才认定 http状态和业务状态均正常 就可以正常的放行通过
        return data["data"]; // 后端统一响应体 {code, msg, data} 只要data结果
      }
      // 抛出异常
      throw DioException(
        requestOptions: res.requestOptions,
        message: data["msg"] ?? "加载数据失败",
      );
    } catch (e) {
      rethrow; // 不改变原来抛出的异常类型
    }
  }
}

// 单例对象
final DioRequest dioRequest = DioRequest();

// dio请求工具发出请求 返回的数据 Response<dynamic>.data
// 把所有的接口的data解放出来 拿到真正的数据 要判断业务状态码是不是等于200
