<template>
  <div class="auth-card w-full max-w-[440px]">
    <div class="mb-7 flex items-center gap-2 text-xs font-semibold text-[#8a8d86]"><RouterLink to="/" class="transition hover:text-ink">Inicio</RouterLink><span class="text-[#c5c6c0]">/</span><span class="text-ink">Recuperación</span></div>
    <div class="mb-8"><div class="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-[#ffe1d9] text-[#d65b47]"><LifeBuoy :size="22" /></div><p class="mb-2 text-xs font-bold uppercase tracking-[.22em] text-[#d65b47]">Pasa hasta en los mejores talleres</p><h2 class="font-display text-3xl font-semibold tracking-tight">Recupera tu acceso</h2><p class="mt-2 text-sm leading-6 text-[#777b74]">Escribe tu correo y te compartiremos los pasos para crear una nueva contraseña.</p></div>
    <form class="space-y-5" @submit.prevent="submit"><label class="block"><span class="mb-2 block text-sm font-semibold">Correo electrónico</span><span class="relative block"><Mail class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><input v-model.trim="email" type="email" autocomplete="email" required placeholder="usuario@example.com" class="field field--icon"/></span></label>
      <div v-if="error" role="alert" class="flex items-start gap-2 rounded-xl border border-[#ff765f]/25 bg-[#fff0ed] px-4 py-3 text-sm text-[#a43f30]"><CircleAlert class="mt-0.5 shrink-0" :size="16"/>{{ error }}</div>
      <div v-if="success" role="status" class="rounded-xl border border-emerald-600/20 bg-emerald-50 px-4 py-3 text-sm leading-5 text-emerald-900"><div class="mb-1 flex items-center gap-2 font-bold"><CircleCheck :size="16"/>Solicitud enviada</div>Si la cuenta existe, recibirás instrucciones para recuperar el acceso.</div>
      <button :disabled="loading || success" class="group flex w-full items-center justify-center gap-2 rounded-2xl bg-ink px-5 py-4 text-sm font-bold text-white shadow-lg shadow-ink/10 transition hover:-translate-y-0.5 hover:bg-[#202b40] disabled:cursor-wait disabled:opacity-70"><LoaderCircle v-if="loading" class="animate-spin" :size="18"/><span>{{ loading ? 'Enviando solicitud…' : 'Enviar instrucciones' }}</span><ArrowRight v-if="!loading" class="transition group-hover:translate-x-1" :size="18"/></button>
    </form><RouterLink to="/" class="mt-7 flex items-center justify-center gap-2 text-sm font-semibold text-[#777b74] transition hover:text-ink"><ArrowLeft :size="16"/>Volver al inicio de sesión</RouterLink>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowLeft, ArrowRight, CircleAlert, CircleCheck, LifeBuoy, LoaderCircle, Mail } from 'lucide-vue-next'
import { forgotPassword } from '../services/auth'
const email=ref(''); const loading=ref(false); const success=ref(false); const error=ref('')
async function submit(){ loading.value=true; error.value=''; try { await forgotPassword(email.value); success.value=true } catch(e){ error.value=e instanceof Error?e.message:'No pudimos enviar la solicitud.' } finally { loading.value=false } }
</script>
<style scoped>.field { width:100%; border:1px solid #e2e3de; background:#fff; border-radius:1rem; padding:.875rem 1rem; font-size:.875rem; outline:none; transition:.2s; } .field--icon { padding-left:2.75rem; } .field:focus { border-color:#a497ef; box-shadow:0 0 0 4px rgb(182 165 255 / .2); } .field::placeholder { color:#b6b8b2; }</style>
