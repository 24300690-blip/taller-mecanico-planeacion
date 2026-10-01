<template>
  <div class="auth-card w-full max-w-[440px]">
    <div class="mb-7 flex items-center gap-2 text-xs font-semibold text-[#8a8d86]"><span>Inicio</span><span class="text-[#c5c6c0]">/</span><span class="text-ink">Acceso</span></div>
    <div class="mb-8"><div class="mb-4 grid h-12 w-12 place-items-center rounded-2xl bg-[#e6e0ff] text-[#7664d3]"><KeyRound :size="22" /></div><p class="mb-2 text-xs font-bold uppercase tracking-[.22em] text-[#8576dc]">Qué bueno verte</p><h2 class="font-display text-3xl font-semibold tracking-tight">Entra a tu taller</h2><p class="mt-2 text-sm text-[#777b74]">Usa tus credenciales para continuar.</p></div>
    <form class="space-y-5" @submit.prevent="submit">
      <label class="block"><span class="mb-2 block text-sm font-semibold">Correo electrónico</span><span class="relative block"><Mail class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><input v-model="email" type="email" autocomplete="username" required placeholder="usuario@example.com" class="w-full rounded-2xl border border-[#e2e3de] bg-white px-4 py-3.5 pl-11 text-sm outline-none transition placeholder:text-[#b6b8b2] focus:border-[#a497ef] focus:ring-4 focus:ring-[#b6a5ff]/20" /></span></label>
      <label class="block"><span class="mb-2 flex items-center justify-between text-sm font-semibold">Contraseña <RouterLink to="/recuperar" class="text-xs font-semibold text-[#786bd0] transition hover:text-ink">¿La olvidaste?</RouterLink></span><span class="relative block"><LockKeyhole class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-[#989c94]" :size="18"/><input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" required placeholder="Tu contraseña" class="w-full rounded-2xl border border-[#e2e3de] bg-white px-4 py-3.5 pl-11 pr-12 text-sm outline-none transition placeholder:text-[#b6b8b2] focus:border-[#a497ef] focus:ring-4 focus:ring-[#b6a5ff]/20"/><button type="button" class="absolute right-4 top-1/2 -translate-y-1/2 text-[#898d86] hover:text-ink" :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'" @click="showPassword = !showPassword"><Eye v-if="!showPassword" :size="18"/><EyeOff v-else :size="18"/></button></span></label>
      <div v-if="error" role="alert" class="flex items-start gap-2 rounded-xl border border-[#ff765f]/25 bg-[#fff0ed] px-4 py-3 text-sm text-[#a43f30]"><CircleAlert class="mt-0.5 shrink-0" :size="16"/>{{ error }}</div>
      <button :disabled="loading" class="group flex w-full items-center justify-center gap-2 rounded-2xl bg-ink px-5 py-4 text-sm font-bold text-white shadow-lg shadow-ink/10 transition hover:-translate-y-0.5 hover:bg-[#202b40] disabled:cursor-wait disabled:opacity-70"><LoaderCircle v-if="loading" class="animate-spin" :size="18"/><span>{{ loading ? 'Verificando acceso…' : 'Entrar al taller' }}</span><ArrowRight v-if="!loading" class="transition group-hover:translate-x-1" :size="18"/></button>
    </form>
    <p v-if="publicRegistration" class="mt-7 text-center text-sm text-[#777b74]">¿Primera vez aquí? <RouterLink to="/registro" class="font-bold text-ink underline decoration-[#d9f36a] decoration-2 underline-offset-4 hover:text-[#7664d3]">Crea tu cuenta</RouterLink></p>
    <div class="mt-7 flex items-center justify-center gap-2 text-[11px] text-[#989b94]"><ShieldCheck :size="15"/> Tus datos se mantienen privados y protegidos.</div>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { ArrowRight, CircleAlert, Eye, EyeOff, KeyRound, LoaderCircle, LockKeyhole, Mail, ShieldCheck } from 'lucide-vue-next'
import { login,publicRegistration } from '../services/auth'
const router = useRouter(); const email = ref(''); const password = ref(''); const showPassword = ref(false); const loading = ref(false); const error = ref('')
async function submit() { loading.value = true; error.value = ''; try { await login(email.value, password.value); await router.push('/inicio') } catch (e) { error.value = e instanceof Error ? e.message : 'No pudimos iniciar sesión.' } finally { loading.value = false } }
</script>
