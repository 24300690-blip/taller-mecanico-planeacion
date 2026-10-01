<script setup lang="ts">
import {computed,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {Home,Users,UserPlus,Shield,Settings,LogOut,Menu,Wrench} from 'lucide-vue-next'
import {authState,logout} from '../services/auth'
import Avatar from './Avatar.vue'
const expanded=ref(false),hover=ref(false),route=useRoute(),router=useRouter(),role=computed(()=>authState.user?.role||'')
/** Navegación según permisos efectivos. */
const links=computed(()=>[{to:'/inicio',label:'Inicio',icon:Home},...(['ADMINISTRADOR','RECEPCIONISTA'].includes(role.value)?[{to:'/clientes/nuevo',label:'Registrar cliente',icon:UserPlus}]:[]),...(['ADMINISTRADOR','RECEPCIONISTA','GERENTE'].includes(role.value)?[{to:'/clientes',label:'Clientes',icon:Users}]:[]),...(role.value==='ADMINISTRADOR'?[{to:'/usuarios',label:'Usuarios y roles',icon:Shield}]:[])])
/** Revoca sesión al salir. */
async function signOut(){await logout();await router.replace('/')}
</script>
<template><div class="app-shell" :class="{'menu-open':expanded||hover}"><button v-if="expanded" class="mobile-scrim" aria-label="Cerrar menú" @click="expanded=false"/><aside class="sidebar" @mouseenter="hover=true" @mouseleave="hover=false"><RouterLink class="brand" to="/inicio"><Wrench/><span class="nav-label">taller uno</span></RouterLink><nav><RouterLink v-for="link in links" :key="link.to" :to="link.to" :title="link.label" :class="{active:route.path===link.to}" @click="expanded=false"><component :is="link.icon" :size="21"/><span class="nav-label">{{link.label}}</span></RouterLink></nav><div class="sidebar-bottom"><RouterLink to="/configuracion" title="Configuración" :class="{active:route.path==='/configuracion'}" @click="expanded=false"><Settings :size="21"/><span class="nav-label">Configuración</span></RouterLink><button title="Salir" @click="signOut"><LogOut :size="21"/><span class="nav-label">Salir</span></button></div></aside><div class="workspace"><header class="topbar"><button class="icon-button" aria-label="Desplegar menú" :aria-expanded="expanded" @click="expanded=!expanded"><Menu/></button><span class="breadcrumb">Taller uno / <strong>{{route.meta.title||'Página'}}</strong></span><div class="identity"><div><strong>{{authState.user?.fullName}}</strong><small>{{role}}</small></div><Avatar/></div></header><div class="page-content"><slot/></div></div></div></template>
