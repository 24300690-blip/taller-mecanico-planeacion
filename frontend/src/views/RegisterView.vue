<template>
  <div class="auth-card w-full max-w-[440px]">
    <div class="mb-6 flex items-center gap-2 text-xs font-semibold text-[#8a8d86]"><RouterLink to="/" class="transition hover:text-ink">Inicio</RouterLink><span class="text-[#c5c6c0]">/</span><span class="text-ink">Crear cuenta</span></div>
    <div class="mb-6"><div class="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-[#e9f4c5] text-[#586817]"><UserRoundPlus :size="22" /></div><p class="mb-2 text-xs font-bold uppercase tracking-[.22em] text-[#768e27]">Un gran equipo empieza aquí</p><h2 class="font-display text-3xl font-semibold tracking-tight">Crea tu acceso</h2><p class="mt-2 text-sm text-[#777b74]">Pide el acceso de tu rol para entrar al taller.</p></div>
    <form class="space-y-4" @submit.prevent="submit">
      <label class="block"><span class="mb-2 block text-sm font-semibold">Nombre completo</span><span class="relative block"><UserRound class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><input v-model.trim="fullName" autocomplete="name" required minlength="2" maxlength="120" placeholder="Tu nombre" class="field field--icon"/></span></label>
      <label class="block"><span class="mb-2 block text-sm font-semibold">Correo electrónico</span><span class="relative block"><Mail class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><input v-model.trim="email" type="email" autocomplete="email" required maxlength="254" placeholder="usuario@example.com" class="field field--icon"/></span></label>
      <label class="block"><span class="mb-2 block text-sm font-semibold">Tu rol en el taller</span><span class="relative block"><BriefcaseBusiness class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><select v-model="role" required class="field field--icon appearance-none"><option value="RECEPCIONISTA">Recepcionista</option><option value="MECANICO">Mecánico</option></select><ChevronDown class="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="17"/></span></label>
      <label class="block"><span class="mb-2 block text-sm font-semibold">Contraseña</span><span class="relative block"><LockKeyhole class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><input v-model="password" type="password" autocomplete="new-password" required minlength="12" maxlength="72" placeholder="Mínimo 12 caracteres" class="field field--icon"/></span><span class="mt-1.5 block text-[11px] text-[#93968f]">12 caracteres como mínimo: mayúscula, minúscula, número y símbolo.</span></label>
      <div v-if="error" role="alert" class="flex items-start gap-2 rounded-xl border border-[#ff765f]/25 bg-[#fff0ed] px-4 py-3 text-sm text-[#a43f30]"><CircleAlert class="mt-0.5 shrink-0" :size="16"/>{{ error }}</div>
      <div v-if="success" role="status" class="flex items-start gap-2 rounded-xl border border-emerald-600/20 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"><CircleCheck class="mt-0.5 shrink-0" :size="16"/>Cuenta creada. Ya puedes <RouterLink to="/" class="font-bold underline">iniciar sesión</RouterLink>.</div>
      <button :disabled="loading || success" class="group flex w-full items-center justify-center gap-2 rounded-2xl bg-ink px-5 py-4 text-sm font-bold text-white shadow-lg shadow-ink/10 transition hover:-translate-y-0.5 hover:bg-[#202b40] disabled:cursor-wait disabled:opacity-70"><LoaderCircle v-if="loading" class="animate-spin" :size="18"/><span>{{ loading ? 'Creando cuenta…' : 'Crear mi cuenta' }}</span><ArrowRight v-if="!loading" class="transition group-hover:translate-x-1" :size="18"/></button>
    </form>
    <p class="mt-5 text-center text-sm text-[#777b74]">¿Ya tienes acceso? <RouterLink to="/" class="font-bold text-[#7664d3] hover:text-ink">Inicia sesión</RouterLink></p>
    <p class="mt-4 text-center text-[11px] text-[#989b94]">Los accesos de administrador se configuran de forma segura.</p>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, BriefcaseBusiness, ChevronDown, CircleAlert, CircleCheck, LoaderCircle, LockKeyhole, Mail, UserRound, UserRoundPlus } from 'lucide-vue-next'
import { register } from '../services/auth'
const fullName=ref(''); const email=ref(''); const password=ref(''); const role=ref('RECEPCIONISTA'); const loading=ref(false); const error=ref(''); const success=ref(false)
async function submit(){
  error.value=''
  const strongPassword=/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{12,72}$/
  if(!strongPassword.test(password.value)){ error.value='La contraseña necesita al menos 12 caracteres e incluir mayúscula, minúscula, número y símbolo.'; return }
  loading.value=true
  try { await register(fullName.value,email.value,password.value,role.value); success.value=true }
  catch(e){ error.value=e instanceof Error?e.message:'No pudimos crear la cuenta. Revisa el correo y los datos.' }
  finally { loading.value=false }
}
</script>
<style scoped>.field { width:100%; border:1px solid #e2e3de; background:#fff; border-radius:1rem; padding:.875rem 1rem; font-size:.875rem; outline:none; transition:.2s; } .field--icon { padding-left:2.75rem; } .field:focus { border-color:#a497ef; box-shadow:0 0 0 4px rgb(182 165 255 / .2); } .field::placeholder { color:#b6b8b2; }</style>
