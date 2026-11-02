<script setup>
defineProps({
  title: { type: String, required: true },
  nextStep: { type: String, required: true },
})
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">MODULE</p>
        <h1>{{ title }}</h1>
        <p>{{ nextStep }}</p>
      </div>
    </div>

    <article class="empty-state">
      <span>BUILD IN PROGRESS</span>
      <h2>骨架已準備好，功能將依 Ticket 實作</h2>
      <p>實作前先定義 User Story、權限、狀態轉換與驗收條件。</p>
    </article>
  </section>
</template>

