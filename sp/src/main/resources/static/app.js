
const { createApp } = Vue;

const IpInfo = {
  template: `
    <div class="container">
      <h1>IP信息查询</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="result">
        <pre>{{ result }}</pre>
      </div>
    </div>
  `,
  data() {
    return {
      result: null,
      loading: true,
      error: null
    }
  },
  mounted() {
    this.fetchIpInfo();
  },
  methods: {
    async fetchIpInfo() {
      try {
        const response = await fetch('http://localhost:8080/up/ip');
        if (!response.ok) throw new Error(`请求失败: ${response.status}`);
        this.result = await response.text();
        console.log("response.data 结果: " + this.result);

        // try {
        //   const res = await fetch('http://localhost:8080/up/ip');
        //   if (!res.ok) throw new Error(`HTTP ${res.status}`);
        //   return res.headers.get('Content-Type').includes('json')
        //       ? await res.json()
        //       : await res.text();
        // } catch (e) {
        //   console.error('Fetch失败:', e);
        // }
      } catch (err) {
        this.error = err.message;
      } finally {
        this.loading = false;
      }
    }
  }
};

createApp(IpInfo).mount('#app');
