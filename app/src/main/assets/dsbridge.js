/**
 * DSBridge JavaScript SDK
 * 用于H5与Native通信
 */
(function() {
    var dsBridge = {
        // 同步调用（核心方法）
        _callSync: function(method, args) {
            var arg = args === undefined || args === null ? '' : args;
            if (typeof args === 'object') {
                arg = JSON.stringify(args);
            }
            var result = window._dsbridge.call(method, JSON.stringify({
                data: arg
            }));
            try {
                var ret = JSON.parse(result);
                if (ret.code === 0) {
                    return ret.data;
                }
                return ret;
            } catch (e) {
                return result;
            }
        },

        // 异步调用
        _callAsync: function(method, args, callback) {
            var cbId = 'cb_' + Date.now() + '_' + Math.random().toString(36).substr(2);
            var arg = args === undefined || args === null ? '' : args;
            if (typeof args === 'object') {
                arg = JSON.stringify(args);
            }
            window[cbId] = function(data) {
                callback && callback(data);
                delete window[cbId];
            };
            window._dsbridge.call(method, JSON.stringify({
                data: arg,
                _dscbstub: cbId
            }));
        },

        // 统一调用方法
        call: function(method, args, callback) {
            if (typeof callback === 'function') {
                return this._callAsync(method, args, callback);
            }
            return this._callSync(method, args);
        },

        // 注册JavaScript方法供Native调用
        register: function(method, handler) {
            window[method] = handler;
        }
    };

    // 暴露到全局
    window.dsBridge = dsBridge;
})();