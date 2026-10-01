<template>
 <main class="register-client stack"><header><span class="eyebrow">Directorio del taller</span><h1>Registrar cliente</h1><p class="muted">Los campos con * son obligatorios.</p></header>
 <form class="stack" @submit.prevent="submit">
  <section v-for="(group,index) in groups" :key="group.title" class="card stack"><h2>{{index+1}} · {{group.title}}</h2><label v-for="field in fields.slice(group.start,group.end)" :key="field.key">{{field.label}}{{field.required?' *':''}}<input v-model.trim="form[field.key]" :type="field.type||'text'" :required="field.required" :maxlength="field.max" :pattern="field.pattern" @invalid="fieldErrors[field.key]=($event.target as HTMLInputElement).validationMessage" @input="fieldErrors[field.key]=''"/><small v-if="fieldErrors[field.key]" class="field-error">{{fieldErrors[field.key]}}</small></label></section>
  <section class="card stack"><h2>4 · Fotografía</h2><div class="drop-zone" @dragover.prevent @drop.prevent="dropPhoto"><label>Arrastra una fotografía o elige un archivo<input type="file" accept="image/jpeg,image/png,image/webp" @change="selectPhoto"/></label><p class="muted">Opcional · JPG, PNG o WEBP · máximo 15 MB</p></div><div v-if="preview" class="profile-inline"><img :src="preview" alt="Vista previa" class="preview-photo"/><button type="button" @click="clearPhoto">Quitar fotografía</button></div></section>
  <p v-if="error" role="alert" class="field-error">{{error}}</p><footer class="savebar"><button type="button" class="secondary" @click="router.push('/inicio')">Cancelar</button><button class="primary" :disabled="loading">{{loading?'Guardando…':'Guardar cliente'}}</button></footer>
 </form></main>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, LoaderCircle } from 'lucide-vue-next'
import Swal from 'sweetalert2'
import { ClienteApiError, ClienteFacade, type ClienteInput } from '../services/ClienteFacade'

const router = useRouter()
const form = reactive<Omit<ClienteInput, 'foto'>>({ nombres:'', apellidoPaterno:'', apellidoMaterno:'', fechaNacimiento:'', telefonoPersonal:'', telefonoTrabajo:'', correoPersonal:'', correoTrabajo:'', calle:'', colonia:'', municipio:'', estado:'', codigoPostal:'' })
const fields = [
  {key:'nombres',label:'Nombre(s)',required:true,max:120,pattern:"[\\p{L}]+(?:\\s+[\\p{L}]+)*"},
  {key:'apellidoPaterno',label:'Apellido paterno',required:true,max:80,pattern:"[\\p{L}]+(?:\\s+[\\p{L}]+)*"},
  {key:'apellidoMaterno',label:'Apellido materno',max:80,pattern:"[\\p{L}]+(?:\\s+[\\p{L}]+)*"},
  {key:'fechaNacimiento',label:'Fecha de nacimiento',required:true,type:'date'},
  {key:'telefonoPersonal',label:'Teléfono personal',required:true,max:10,pattern:'[0-9]{10}',type:'tel'},
  {key:'telefonoTrabajo',label:'Teléfono de trabajo',max:20,type:'tel'},
  {key:'correoPersonal',label:'Correo personal',required:true,max:254,type:'email'},
  {key:'correoTrabajo',label:'Correo de trabajo',max:254,type:'email'},
  {key:'calle',label:'Calle(s)',required:true,max:180,wide:true},
  {key:'colonia',label:'Colonia',required:true,max:120,pattern:"[\\p{L}]+(?:\\s+[\\p{L}]+)*"},
  {key:'municipio',label:'Municipio',required:true,max:120,pattern:"[\\p{L}]+(?:\\s+[\\p{L}]+)*"},
  {key:'estado',label:'Estado',required:true,max:120,pattern:"[\\p{L}]+(?:\\s+[\\p{L}]+)*"},
  {key:'codigoPostal',label:'Código postal',required:true,max:5,pattern:'[0-9]{5}'},
] as const
const groups=[{title:'Datos personales',start:0,end:4},{title:'Contacto',start:4,end:8},{title:'Dirección',start:8,end:13}]
const fieldErrors=reactive<Record<string,string>>({})
/** Admite arrastrar una foto usando el mismo validador que el selector. */
function dropPhoto(event:DragEvent){const file=event.dataTransfer?.files?.[0];if(file)selectPhoto({target:{files:[file],value:''}} as unknown as Event)}
const photo = ref<File>()
const preview = ref('')
const loading = ref(false)
const error = ref('')
const success = ref<{id:number} | null>(null)

/** Libera la URL de previsualización al cerrar o reemplazar la foto. */
function clearPhoto() { if (preview.value) URL.revokeObjectURL(preview.value); preview.value=''; photo.value=undefined }
/** Verifica tamaño y tipo declarado antes de mostrar vista previa. */
function selectPhoto(event: Event) {
  clearPhoto()
  const file=(event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 15*1024*1024 || !['image/jpeg','image/png','image/webp'].includes(file.type)) { error.value='Selecciona JPG, PNG o WEBP de hasta 15 MB.'; (event.target as HTMLInputElement).value=''; return }
  photo.value=file; preview.value=URL.createObjectURL(file); error.value=''
}
/** Valida los datos, confirma y registra el cliente mediante ClienteFacade. */
async function submit() {
  error.value=''; success.value=null
  const birth=new Date(`${form.fechaNacimiento}T00:00:00`)
  const adultLimit=new Date(); adultLimit.setFullYear(adultLimit.getFullYear()-18)
  if (!form.fechaNacimiento || birth > adultLimit) { error.value='El cliente debe tener al menos 18 años.'; fieldErrors.fechaNacimiento=error.value; return }
  const confirm=await Swal.fire({title:'¿Guardar cliente?',text:'Se registrarán los datos capturados.',icon:'question',showCancelButton:true,confirmButtonText:'Guardar',cancelButtonText:'Revisar'})
  if (!confirm.isConfirmed) return
  loading.value=true
  try {
    success.value=await ClienteFacade.create({...form, ...(photo.value ? {foto:photo.value} : {})})
    await Swal.fire({title:'Cliente guardado',text:`El cliente #${success.value.id} quedó registrado.`,icon:'success',confirmButtonText:'Aceptar'})
    await router.push('/clientes')
  } catch (cause) {
    error.value=cause instanceof Error ? cause.message : 'No fue posible guardar el cliente.'
    if (cause instanceof ClienteApiError && cause.status===409) await Swal.fire({title:'Cliente duplicado',text:error.value,icon:'warning',confirmButtonText:'Entendido'})
    else await Swal.fire({title:'No se pudo guardar',text:error.value,icon:'error',confirmButtonText:'Cerrar'})
  } finally { loading.value=false }
}
onBeforeUnmount(clearPhoto)
</script>

<style scoped>.field{width:100%;border:1px solid #e2e3de;background:#fff;border-radius:1rem;padding:.8rem 1rem;font-size:.875rem;outline:none;transition:.2s}.field:focus{border-color:#a497ef;box-shadow:0 0 0 4px rgb(182 165 255 / .2)}</style>
