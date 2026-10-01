<script setup lang="ts">
import {computed,ref} from 'vue'
import Swal from 'sweetalert2'
import {changePassword} from '../services/auth'
defineProps<{first?:boolean}>();const emit=defineEmits<{done:[];cancel:[]}>(),current=ref(''),password=ref(''),confirm=ref(''),busy=ref(false),error=ref('')
/** Requisitos en vivo antes del envío. */
const rules=computed(()=>[{label:'De 12 a 72 caracteres',ok:password.value.length>=12&&new TextEncoder().encode(password.value).length<=72},{label:'Mayúscula y minúscula',ok:/[A-Z]/.test(password.value)&&/[a-z]/.test(password.value)},{label:'Número y símbolo',ok:/\d/.test(password.value)&&/[^A-Za-z0-9]/.test(password.value)},{label:'La confirmación coincide',ok:!!confirm.value&&confirm.value===password.value}])
/** Guarda contraseña con validación definitiva en servidor. */
async function submit(){busy.value=true;error.value='';try{await changePassword(current.value,password.value,confirm.value);await Swal.fire({icon:'success',title:'Contraseña actualizada'});emit('done')}catch(e){error.value=(e as Error).message;await Swal.fire({icon:'error',title:'No se pudo cambiar',text:error.value})}finally{busy.value=false}}
</script>
<template><form class="stack" @submit.prevent="submit"><label>{{first?'Contraseña temporal':'Contraseña actual'}}<input v-model="current" type="password" autocomplete="current-password" required/></label><label>Nueva contraseña<input v-model="password" type="password" autocomplete="new-password" required maxlength="72"/></label><label>Confirmar contraseña<input v-model="confirm" type="password" autocomplete="new-password" required maxlength="72"/></label><ul class="requirements"><li v-for="rule in rules" :key="rule.label" :class="{met:rule.ok}">{{rule.ok?'✓':'○'}} {{rule.label}}</li></ul><p v-if="error" role="alert" class="field-error">{{error}}</p><div class="actions"><button v-if="!first" type="button" class="secondary" @click="emit('cancel')">Cancelar</button><button class="primary" :disabled="busy||rules.some(r=>!r.ok)">{{busy?'Guardando…':first?'Guardar y continuar':'Guardar contraseña'}}</button></div></form></template>
