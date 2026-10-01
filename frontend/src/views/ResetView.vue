<template>
  <div class="auth-card w-full max-w-[440px]">
    <div class="mb-7 flex items-center gap-2 text-xs font-semibold text-[#8a8d86]"><RouterLink to="/" class="transition hover:text-ink">Inicio</RouterLink><span class="text-[#c5c6c0]">/</span><span class="text-ink">Nueva contraseña</span></div>
    <div class="mb-8"><div class="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-[#e6e0ff] text-[#7664d3]"><KeyRound :size="22" /></div><p class="mb-2 text-xs font-bold uppercase tracking-[.22em] text-[#8576dc]">Ya casi estás</p><h2 class="font-display text-3xl font-semibold tracking-tight">Crea una nueva clave</h2><p class="mt-2 text-sm leading-6 text-[#777b74]">Elige una contraseña segura que no uses en otros sitios.</p></div>
    <form class="space-y-5" @submit.prevent="submit"><label class="block"><span class="mb-2 block text-sm font-semibold">Nueva contraseña</span><input v-model="password" type="password" autocomplete="new-password" required minlength="12" maxlength="72" placeholder="12 caracteres o más" class="field"/><span class="mt-1.5 block text-[11px] text-[#93968f]">Incluye mayúscula, minúscula, número y símbolo.</span></label><label class="block"><span class="mb-2 block text-sm font-semibold">Confirma tu contraseña</span><input v-model="confirm" type="password" autocomplete="new-password" required minlength="12" maxlength="72" placeholder="Escríbela de nuevo" class="field"/></label>
      <div v-if="error" role="alert" class="flex items-start gap-2 rounded-xl border border-[#ff765f]/25 bg-[#fff0ed] px-4 py-3 text-sm text-[#a43f30]"><CircleAlert class="mt-0.5 shrink-0" :size="16"/>{{ error }}</div>
      <div v-if="success" role="status" class="rounded-xl border border-emerald-600/20 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"><div class="mb-1 flex items-center gap-2 font-bold"><CircleCheck :size="16"/>Contraseña actualizada</div>Ya puedes entrar con tu nueva contraseña.</div>
      <button :disabled="loading || success || !token" class="group flex w-full items-center justify-center gap-2 rounded-2xl bg-ink px-5 py-4 text-sm font-bold text-white shadow-lg shadow-ink/10 transition hover:-translate-y-0.5 hover:bg-[#202b40] disabled:cursor-not-allowed disabled:opacity-60"><LoaderCircle v-if="loading" class="animate-spin" :size="18"/><span>{{ loading ? 'Guardando…' : 'Guardar nueva contraseña' }}</span><ArrowRight v-if="!loading" class="transition group-hover:translate-x-1" :size="18"/></button>
    </form><RouterLink to="/" class="mt-7 flex items-center justify-center gap-2 text-sm font-semibold text-[#777b74] transition hover:text-ink"><ArrowLeft :size="16"/>Volver al inicio de sesión</RouterLink>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, CircleAlert, CircleCheck, KeyRound, LoaderCircle } from 'lucide-vue-next'
import { resetPassword } from '../services/auth'
const route=useRoute(); const token=typeof route.query.token==='string'?route.query.token:''; const password=ref(''); const confirm=ref(''); const loading=ref(false); const success=ref(false); const error=ref(token?'':'El enlace de recuperación no contiene un token válido.')
async function submit(){ if(password.value!==confirm.value){ error.value='Las contraseñas no coinciden.'; return }; loading.value=true; error.value=''; try { await resetPassword(token,password.value); success.value=true } catch(e){ error.value=e instanceof Error?e.message:'El enlace expiró o ya se utilizó.' } finally { loading.value=false } }
</script>
<style scoped>.field { width:100%; border:1px solid #e2e3de; background:#fff; border-radius:1rem; padding:.875rem 1rem; font-size:.875rem; outline:none; transition:.2s; } .field:focus { border-color:#a497ef; box-shadow:0 0 0 4px rgb(182 165 255 / .2); } .field::placeholder { color:#b6b8b2; }</style>
