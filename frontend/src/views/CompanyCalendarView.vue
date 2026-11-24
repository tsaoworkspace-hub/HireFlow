<script setup>
import { computed,onMounted,reactive,ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getInterviewOptions } from '@/services/interviewService'
import { createInterviewSlot,getCalendarEvents } from '@/services/calendarService'
const auth=useAuthStore(),events=ref([]),options=ref({applications:[],interviewers:[]}),error=ref(''),message=ref(''),loading=ref(false)
const now=new Date(),range=reactive({from:new Date(now.getFullYear(),now.getMonth(),1).toISOString(),to:new Date(now.getFullYear(),now.getMonth()+1,1).toISOString()})
const form=reactive({applicationId:'',roundNumber:1,startsAt:'',endsAt:'',format:'ONLINE',location:'',interviewerIds:[]})
const canPropose=computed(()=>auth.hasAnyRole(['HR']))
async function load(){loading.value=true;error.value='';try{events.value=await getCalendarEvents(range.from,range.to);if(canPropose.value)options.value=await getInterviewOptions()}catch(e){error.value=e.response?.data?.message||'無法取得行事曆'}finally{loading.value=false}}
async function propose(){try{await createInterviewSlot({...form,applicationId:Number(form.applicationId),roundNumber:Number(form.roundNumber),startsAt:new Date(form.startsAt).toISOString(),endsAt:new Date(form.endsAt).toISOString(),interviewerIds:form.interviewerIds.map(Number)});message.value='候選面試時段已寄出通知';await load()}catch(e){error.value=e.response?.data?.message||'建立時段失敗'}}
onMounted(load)
</script>
<template><section><div class="page-heading"><div><p class="eyebrow">COMPANY CALENDAR</p><h1>公司面試行事曆</h1><p>HR 可跨部門管理；主管只看自己部門；面試官只看指派給自己的行程。</p></div></div>
<p v-if="error" class="alert-message is-error">{{error}}</p><p v-if="message" class="alert-message is-success">{{message}}</p>
<form v-if="canPropose" class="form-card calendar-slot-form" @submit.prevent="propose"><h2>提供求職者可選時段</h2><div class="form-grid"><label>應徵紀錄<select v-model="form.applicationId" required><option value="">請選擇</option><option v-for="a in options.applications" :key="a.id" :value="a.id">{{a.candidateName}}｜{{a.jobTitle}}</option></select></label><label>輪次<input v-model="form.roundNumber" type="number" min="1" required></label><label>開始<input v-model="form.startsAt" type="datetime-local" required></label><label>結束<input v-model="form.endsAt" type="datetime-local" required></label><label>形式<select v-model="form.format"><option value="ONLINE">線上</option><option value="ONSITE">實體</option><option value="PHONE">電話</option></select></label><label>地點／連結<input v-model.trim="form.location"></label><label class="span-full">面試官<select v-model="form.interviewerIds" multiple required><option v-for="u in options.interviewers" :key="u.id" :value="u.id">{{u.displayName}}｜{{u.departmentName}}</option></select></label></div><button class="primary-button page-action">新增候選時段</button></form>
<article class="data-card calendar-list"><div v-if="loading" class="table-state">載入中…</div><div v-else-if="!events.length" class="table-state">本月沒有面試行程</div><div v-else v-for="event in events" :key="event.id" class="calendar-row"><time>{{new Date(event.startsAt).toLocaleString('zh-TW')}}</time><div><strong>{{event.candidateName}}｜{{event.jobTitle}}</strong><p>{{event.departmentName}} · {{event.eventType==='PROPOSED'?'候選時段':'已確認'}} · {{event.location||'地點待確認'}}</p></div></div></article></section></template>
