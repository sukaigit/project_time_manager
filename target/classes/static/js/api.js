const API = {
    base: '',
    async request(path, options = {}) {
        const token = localStorage.getItem('token');
        const headers = { 'Content-Type': 'application/json', ...options.headers };
        if (token) headers['Authorization'] = 'Bearer ' + token;

        // 支持获取响应头：如果 options.rawResponse 为 true，返回完整 Response 对象
        if (options.rawResponse) {
            const res = await fetch(API.base + path, { ...options, headers });
            if (res.status === 401) { localStorage.clear(); window.location.href = '/login.html'; }
            return res;
        }

        const res = await fetch(API.base + path, { ...options, headers });
        if (res.status === 401) { localStorage.clear(); window.location.href = '/login.html'; }
        return res.json();
    },
    get(path) { return this.request(path); },
    post(path, data) { return this.request(path, { method: 'POST', body: JSON.stringify(data) }); },
    put(path, data) { return this.request(path, { method: 'PUT', body: JSON.stringify(data) }); },
    del(path) { return this.request(path, { method: 'DELETE' }); }
};
