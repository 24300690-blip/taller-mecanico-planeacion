<template>
  <AppShell v-if="route.meta.protected && route.path!=='/cambiar-contrasena'"><RouterView/></AppShell>
  <RouterView v-else-if="route.path==='/cambiar-contrasena'"/>
  <main v-else class="auth-layout min-h-screen overflow-hidden bg-[#f1f2ee]">
    <div class="mx-auto grid min-h-screen max-w-[1440px] lg:grid-cols-[1.04fr_.96fr]">
      <section class="relative hidden overflow-hidden bg-ink p-12 text-white lg:flex lg:flex-col lg:justify-between xl:p-16">
        <div class="absolute -right-32 top-20 h-96 w-96 rounded-full bg-[#b6a5ff]/20 blur-3xl"></div>
        <div class="absolute -bottom-24 -left-20 h-80 w-80 rounded-full bg-[#d9f36a]/15 blur-3xl"></div>
        <div class="relative z-10 flex items-center gap-3">
          <div class="grid h-11 w-11 place-items-center rounded-2xl bg-citrus text-ink"><Wrench :size="22" :stroke-width="2.5" /></div>
          <span class="font-display text-xl font-bold tracking-tight">taller<span class="text-citrus">uno</span></span>
          <span class="ml-2 rounded-full border border-white/15 px-3 py-1 text-[10px] font-bold uppercase tracking-[.22em] text-white/55">workspace</span>
        </div>
        <div class="relative z-10 max-w-xl pb-6">
          <div class="mb-6 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[.06] px-3 py-2 text-xs text-white/70"><span class="h-2 w-2 rounded-full bg-citrus shadow-[0_0_12px_#d9f36a]"></span> Todo el taller, en sintonía</div>
          <h1 class="font-display text-5xl font-semibold leading-[1.04] tracking-[-.045em] xl:text-6xl">El trabajo bien hecho<br/><span class="text-citrus">empieza aquí.</span></h1>
          <p class="mt-6 max-w-md text-base leading-7 text-white/55">Una nueva forma de mantener cada reparación en movimiento. Entra a tu espacio de trabajo.</p>
          <div class="mt-12 grid max-w-md grid-cols-3 gap-3">
            <div v-for="item in metrics" :key="item.value" class="rounded-2xl border border-white/10 bg-white/[.04] p-4 transition hover:-translate-y-1 hover:bg-white/[.08]"><div class="font-display text-xl font-semibold text-white">{{ item.value }}</div><div class="mt-1 text-[11px] text-white/45">{{ item.label }}</div></div>
          </div>
        </div>
        <div class="relative z-10 flex items-center justify-between text-xs text-white/35"><span>Hecho para mantener las cosas en marcha.</span><span>© 2026 Taller Uno</span></div>
      </section>
      <section class="flex min-h-screen flex-col px-5 py-6 sm:px-10 lg:px-12 xl:px-20">
        <header class="mb-8 flex items-center justify-between lg:justify-end">
          <div class="flex items-center gap-2 lg:hidden"><span class="grid h-9 w-9 place-items-center rounded-xl bg-ink text-citrus"><Wrench :size="18" /></span><span class="font-display font-bold">taller<span class="text-[#7f70da]">uno</span></span></div>
          <div class="flex items-center gap-2 rounded-full border border-black/5 bg-white/70 px-3 py-2 text-xs text-[#72766f]"><span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> Sistema operativo</div>
        </header>
        <div class="flex flex-1 items-center justify-center pb-8"><RouterView /></div>
        <footer class="flex flex-wrap items-center justify-between gap-3 pt-3 text-[11px] text-[#8b8e87]"><span>© 2026 Taller Uno</span><span class="flex items-center gap-4"><a href="#" class="transition hover:text-ink">Privacidad</a><a href="#" class="transition hover:text-ink">Ayuda</a><span class="flex items-center gap-1"><ShieldCheck :size="13" /> Acceso protegido</span></span></footer>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { RouterView,useRoute } from 'vue-router'
import AppShell from './components/AppShell.vue'
const route=useRoute()
import { ShieldCheck, Wrench } from 'lucide-vue-next'
const metrics = [{ value: '01', label: 'Un solo lugar' }, { value: '24/7', label: 'Siempre al día' }, { value: '100%', label: 'Bajo control' }]
</script>
