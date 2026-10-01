<template>
  <main class="mx-auto w-full max-w-7xl px-5 py-8 sm:px-8">
<form class="searchbar" @submit.prevent="loadPage(0)"><label>Buscar clientes<input v-model="query" placeholder="Nombre, teléfono o correo" /></label><button class="primary">Buscar</button></form>
    <section class="auth-card overflow-hidden rounded-[2rem] border border-black/5 bg-white shadow-sm">
      <div class="flex flex-wrap items-end justify-between gap-4 border-b border-[#f0f0ed] px-6 py-7 sm:px-8">
        <div><p class="text-xs font-bold uppercase tracking-[.2em] text-[#768e27]">Directorio del taller</p><h1 class="mt-2 font-display text-3xl font-semibold tracking-tight">Clientes</h1><p class="mt-2 text-sm text-[#777b74]">{{ totalLabel }}</p></div>
        <RouterLink v-if="canCreate" to="/clientes/nuevo" class="rounded-xl bg-[#202a3e] px-4 py-3 text-sm font-bold text-white transition hover:bg-[#303c54]">Registrar cliente</RouterLink>
      </div>

      <div v-if="loading" class="grid min-h-64 place-items-center text-sm font-semibold text-[#777b74]" role="status">Cargando clientes…</div>
      <div v-else-if="errorMessage" class="m-6 rounded-2xl bg-[#fff0ed] p-5 text-sm text-[#b24435]" role="alert">{{ errorMessage }}</div>
      <div v-else-if="!clients.length" class="grid min-h-64 place-items-center px-6 text-center"><div><span class="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-[#edf4fb] text-[#486b89]"><Users :size="24"/></span><h2 class="mt-4 font-display text-xl font-semibold">Aún no hay clientes</h2><p class="mt-2 text-sm text-[#777b74]">Cuando se registren, aparecerán aquí.</p></div></div>
      <template v-else>
        <div class="overflow-x-auto">
          <table class="w-full min-w-[820px] border-collapse text-left">
            <thead class="bg-[#f7f8f4] text-xs uppercase tracking-wide text-[#777b74]"><tr><th class="px-6 py-4 font-bold">Cliente</th><th class="px-5 py-4 font-bold">Teléfono</th><th class="px-5 py-4 font-bold">Correo</th><th class="px-5 py-4 font-bold">Municipio</th></tr></thead>
            <tbody class="divide-y divide-[#f0f0ed]">
              <tr v-for="client in clients" :key="client.id" tabindex="0" class="cursor-pointer transition hover:bg-[#f8faef] focus:bg-[#f8faef] focus:outline-none" @click="openDetail(client.id)" @keydown.enter="openDetail(client.id)" @keydown.space.prevent="openDetail(client.id)" :aria-label="`Ver detalle de ${fullName(client)}`">
                <td class="px-6 py-4"><div class="flex items-center gap-3"><img v-if="photos[client.id]" :src="photos[client.id]" alt="" class="h-11 w-11 rounded-full object-cover ring-2 ring-white"/><span v-else class="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-[#eff4dc] text-sm font-bold text-[#586817]">{{ initials(client) }}</span><div class="font-semibold text-[#202a3e]">{{ fullName(client) }}</div></div></td>
                <td class="px-5 py-4 text-sm text-[#62675f]">{{ client.telefonoPersonal }}</td><td class="px-5 py-4 text-sm text-[#62675f]">{{ client.correoPersonal }}</td><td class="px-5 py-4 text-sm text-[#62675f]">{{ client.municipio }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer class="flex items-center justify-between border-t border-[#f0f0ed] px-6 py-4 sm:px-8"><span class="text-sm text-[#777b74]">Mostrando {{ page*5+1 }}–{{ Math.min((page+1)*5,totalElements) }} de {{totalElements}}</span><div class="flex gap-2"><button type="button" :disabled="page === 0 || loading" @click="loadPage(page - 1)" class="rounded-xl border border-[#e2e3de] px-4 py-2 text-sm font-semibold transition enabled:hover:bg-[#f7f8f4] disabled:cursor-not-allowed disabled:opacity-40">Anterior</button><button type="button" :disabled="page + 1 >= totalPages || loading" @click="loadPage(page + 1)" class="rounded-xl border border-[#e2e3de] px-4 py-2 text-sm font-semibold transition enabled:hover:bg-[#f7f8f4] disabled:cursor-not-allowed disabled:opacity-40">Siguiente</button></div></footer>
      </template>
    </section>

    <div v-if="selected" class="overlay drawer-overlay" role="presentation" @click.self="selected = null">
      <section class="drawer" role="dialog" aria-modal="true" aria-labelledby="client-detail-title">
        <div class="flex items-start justify-between gap-4"><div><p class="text-xs font-bold uppercase tracking-[.2em] text-[#768e27]">Ficha de cliente</p><h2 id="client-detail-title" class="mt-2 font-display text-2xl font-semibold">{{ fullName(selected) }}</h2></div><button type="button" class="rounded-lg px-3 py-1 text-2xl text-[#777b74] hover:bg-[#f7f8f4]" aria-label="Cerrar detalle" @click="selected = null">×</button></div>
        <div class="mt-6 flex items-center gap-4 rounded-2xl bg-[#f7f8f4] p-4"><img v-if="selectedPhoto" :src="selectedPhoto" alt="Fotografía del cliente" class="h-20 w-20 rounded-2xl object-cover"/><span v-else class="grid h-20 w-20 place-items-center rounded-2xl bg-[#eff4dc] text-xl font-bold text-[#586817]">{{ initials(selected) }}</span><div><div class="font-semibold">{{ fullName(selected) }}</div><div class="mt-1 text-sm text-[#777b74]">{{ selected.fechaNacimiento }}</div></div></div>
        <div class="mt-6 grid gap-4 sm:grid-cols-2"><div v-for="item in detailFields(selected)" :key="item.label" class="rounded-2xl border border-[#eeefea] p-4"><div class="text-xs font-bold uppercase tracking-wide text-[#8b8e87]">{{ item.label }}</div><div class="mt-1 break-words text-sm font-semibold text-[#202a3e]">{{ item.value || '—' }}</div></div></div>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Users } from 'lucide-vue-next'
import Swal from 'sweetalert2'
import { authState } from '../services/auth'
import { ClienteFacade, type Cliente } from '../services/ClienteFacade'

const clients = ref<Cliente[]>([])
const photos = ref<Record<number, string>>({})
const selected = ref<Cliente | null>(null)
const selectedPhoto = ref('')
const query=ref('')
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const canCreate = computed(() => ['ADMINISTRADOR', 'RECEPCIONISTA'].includes(authState.user?.role || ''))
const totalLabel = computed(() => `${totalElements.value} ${totalElements.value === 1 ? 'cliente registrado' : 'clientes registrados'}`)

/** Lee una página desde ClienteFacade y carga sus fotos protegidas. */
async function loadPage(target: number): Promise<void> {
  loading.value = true; errorMessage.value = ''; revokePhotos()
  try {
    const result = await ClienteFacade.list(target, 5, query.value)
    clients.value = result.content; page.value = target; totalPages.value = Math.max(1, result.totalPages); totalElements.value = result.totalElements
    const entries = await Promise.all(result.content.map(async client => {
      if (!client.fotoPath) return [client.id, ''] as const
      try { return [client.id, await ClienteFacade.photo(client.id)] as const } catch { return [client.id, ''] as const }
    }))
    photos.value = Object.fromEntries(entries)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'No fue posible cargar los clientes.'
    await Swal.fire({ icon: 'error', title: 'Error al cargar clientes', text: errorMessage.value, confirmButtonColor: '#202a3e' })
  } finally { loading.value = false }
}

/** Obtiene el detalle completo del cliente usando únicamente ClienteFacade. */
async function openDetail(id: number): Promise<void> {
  try {
    selected.value = await ClienteFacade.get(id); selectedPhoto.value = photos.value[id] || ''
    if (!selectedPhoto.value && selected.value.fotoPath) selectedPhoto.value = await ClienteFacade.photo(id)
  } catch (error) {
    await Swal.fire({ icon: 'error', title: 'No se pudo abrir el cliente', text: error instanceof Error ? error.message : 'Intenta de nuevo.', confirmButtonColor: '#202a3e' })
  }
}

/** Construye el nombre completo para tabla y ficha. */
function fullName(client: Cliente): string { return [client.nombres, client.apellidoPaterno, client.apellidoMaterno].filter(Boolean).join(' ') }

/** Genera iniciales cuando el cliente no tiene fotografía. */
function initials(client: Cliente): string { return [client.nombres, client.apellidoPaterno].filter(Boolean).map(part => part[0]).join('').toUpperCase() }

/** Lista los datos completos devueltos por la API para el modal de detalle. */
function detailFields(client: Cliente): Array<{ label: string; value: string | null }> {
  return [
    { label: 'Teléfono personal', value: client.telefonoPersonal }, { label: 'Teléfono de trabajo', value: client.telefonoTrabajo },
    { label: 'Correo personal', value: client.correoPersonal }, { label: 'Correo de trabajo', value: client.correoTrabajo },
    { label: 'Calle', value: client.calle }, { label: 'Colonia', value: client.colonia }, { label: 'Municipio', value: client.municipio },
    { label: 'Estado', value: client.estado }, { label: 'Código postal', value: client.codigoPostal },
  ]
}

/** Libera las URLs temporales de fotos creadas desde respuestas blob. */
function revokePhotos(): void { Object.values(photos.value).forEach(url => { if (url) URL.revokeObjectURL(url) }); photos.value = {} }

onMounted(() => loadPage(0))
onBeforeUnmount(() => { revokePhotos(); if (selectedPhoto.value) URL.revokeObjectURL(selectedPhoto.value) })
</script>
