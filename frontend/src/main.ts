import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import './style.css'
import App from './App.vue'
import LoginView from './views/LoginView.vue'
import RegisterView from './views/RegisterView.vue'
import RecoveryView from './views/RecoveryView.vue'
import ResetView from './views/ResetView.vue'
import DashboardView from './views/DashboardView.vue'
import ClienteRegisterView from './views/ClienteRegisterView.vue'
import ClientesView from './views/ClientesView.vue'
import UsuariosView from './views/UsuariosView.vue'
import SettingsView from './views/SettingsView.vue'
import ChangePasswordView from './views/ChangePasswordView.vue'
import ErrorView from './views/ErrorView.vue'
import { authState, restoreSession, publicRegistration } from './services/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: LoginView },
    { path: '/registro', component: RegisterView },
    { path: '/recuperar', component: RecoveryView },
    { path: '/reset-password', component: ResetView },
    { path: '/inicio', component: DashboardView, meta: { protected: true,title:'Inicio' } },
    { path: '/clientes/nuevo', component: ClienteRegisterView, meta: { protected: true, title:'Registrar cliente',roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] } },
    { path: '/clientes', component: ClientesView, meta: { protected: true, title:'Clientes',roles: ['ADMINISTRADOR', 'RECEPCIONISTA', 'GERENTE'] } },
    {path:'/usuarios',component:UsuariosView,meta:{protected:true,title:'Usuarios y roles',roles:['ADMINISTRADOR']}},
    {path:'/configuracion',component:SettingsView,meta:{protected:true,title:'Configuración'}},
    {path:'/cambiar-contrasena',component:ChangePasswordView,meta:{protected:true,title:'Primer acceso'}},
    {path:'/403',component:ErrorView,meta:{protected:true,title:'Acceso denegado'}},
    {path:'/:pathMatch(.*)*',component:ErrorView,meta:{protected:true,title:'Página no encontrada'}},
  ],
})
router.beforeEach(async (to) => {
  if (to.meta.protected && !authState.accessToken) await restoreSession()
  if (to.meta.protected && !authState.accessToken) return '/'
  if(authState.mustChangePassword && to.path!=='/cambiar-contrasena')return '/cambiar-contrasena'
  if(!authState.mustChangePassword && to.path==='/cambiar-contrasena')return authState.accessToken?'/inicio':'/'
  if(to.path==='/registro'&&!publicRegistration)return '/'
  const allowedRoles = (to.meta as { roles?: string[] }).roles
  if (allowedRoles && !allowedRoles.includes(authState.user?.role || '')) return '/403'
})
createApp(App).use(router).mount('#app')
