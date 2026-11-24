<template>
  <section class="empty-state forbidden-state">
    <span>403 · FORBIDDEN</span>
    <h1>目前角色沒有這個功能的權限</h1>
    <p>導覽會依角色顯示；後端也會對每個受保護操作重新驗證。</p>
    <RouterLink class="primary-button inline-button" to="/">返回 Dashboard</RouterLink>
  </section>
</template>
