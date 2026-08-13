const state = {
  token: sessionStorage.getItem('token') || '',
  user: JSON.parse(sessionStorage.getItem('user') || 'null'),
  project: null,
  meeting: null,
  questions: []
};
const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];
const esc = (v='') => String(v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

async function api(path, options={}) {
  const headers = {...(options.headers || {})};
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  if (options.body && !(options.body instanceof FormData)) headers['Content-Type'] = 'application/json';
  const started = Date.now();
  let response, payload;
  try {
    response = await fetch(path, {...options, headers});
    payload = await response.json();
  } catch (e) {
    log({request:path,error:e.message}); throw e;
  }
  log({request:`${options.method || 'GET'} ${path}`,status:response.status,time:`${Date.now()-started}ms`,response:payload});
  if (!response.ok || payload.success === false) {
    const message = payload?.error?.message || `요청 실패 (${response.status})`;
    toast(message); throw new Error(message);
  }
  return payload.data;
}
function log(value){ $('#log').textContent=JSON.stringify(value,null,2); }
function toast(message){ const el=$('#toast');el.textContent=message;el.classList.add('show');setTimeout(()=>el.classList.remove('show'),2600); }
function formData(form){ return Object.fromEntries(new FormData(form)); }
function selectStep(id){ $$('.panel,nav button').forEach(x=>x.classList.remove('active'));$('#'+id)?.classList.add('active');$(`nav button[data-step="${id}"]`)?.classList.add('active'); }
function saveAuth(data){ state.token=data.accessToken;state.user=data;sessionStorage.setItem('token',state.token);sessionStorage.setItem('user',JSON.stringify(data));renderAuth(); }
function renderAuth(){ const b=$('#authBadge');b.textContent=state.user?`${state.user.name} 로그인됨`:'로그인 필요';b.classList.toggle('ok',!!state.user); }
function setProject(project){state.project=project;$$('[data-project]').forEach(x=>x.textContent=`#${project.id} ${project.name}`);renderParticipants(project.members||[]);toast(`${project.name} 선택됨`);}
function setMeeting(meeting){state.meeting=meeting;$$('[data-meeting]').forEach(x=>x.textContent=`#${meeting.id} ${meeting.title}`);toast(`${meeting.title} 선택됨`);}
function requireState(key){if(!state[key]){toast(`${key==='project'?'프로젝트':'회의'}를 먼저 선택하세요.`);return false}return true}

$$('nav button').forEach(b=>b.onclick=()=>selectStep(b.dataset.step));
$$('[data-go]').forEach(b=>b.onclick=()=>{selectStep(b.dataset.go);if(b.dataset.go==='history')loadHistory();});
$('#clearLog').onclick=()=>$('#log').textContent='응답 기록을 지웠습니다.';
$('#signupForm').onsubmit=async e=>{e.preventDefault();try{saveAuth(await api('/api/v1/auth/signup',{method:'POST',body:JSON.stringify(formData(e.target))}));selectStep('project');await loadProjects();}catch{}};
$('#loginForm').onsubmit=async e=>{e.preventDefault();try{saveAuth(await api('/api/v1/auth/login',{method:'POST',body:JSON.stringify(formData(e.target))}));selectStep('project');await loadProjects();}catch{}};
$('#logoutButton').onclick=()=>{state.token='';state.user=null;sessionStorage.clear();renderAuth();toast('로그아웃했습니다.');};

async function loadProjects(){try{renderProjects(await api('/api/v1/projects'));}catch{}}
function renderProjects(items){const el=$('#projectList');if(!items.length){el.className='list empty';el.textContent='프로젝트가 없습니다.';return}el.className='list';el.innerHTML=items.map(p=>`<div class="list-item ${state.project?.id===p.id?'selected':''}"><div><b>#${p.id} ${esc(p.name)}</b><p>${esc(p.description)} · ${p.myRole}</p></div><button data-pid="${p.id}">선택</button></div>`).join('');$$('[data-pid]').forEach(b=>b.onclick=async()=>{setProject(await api(`/api/v1/projects/${b.dataset.pid}`));renderProjects(items)});}
$('#loadProjects').onclick=loadProjects;
$('#projectForm').onsubmit=async e=>{e.preventDefault();try{const p=await api('/api/v1/projects',{method:'POST',body:JSON.stringify(formData(e.target))});setProject(p);await loadProjects();}catch{}};
$('#joinForm').onsubmit=async e=>{e.preventDefault();try{const p=await api('/api/v1/projects/join',{method:'POST',body:JSON.stringify({joinCode:new FormData(e.target).get('joinCode').toUpperCase()})});setProject(p);await loadProjects();}catch{}};
function renderParticipants(members){$('#participants').innerHTML=members.map(m=>`<label><input type="checkbox" name="participant" value="${m.userId}" ${m.userId===state.user?.userId?'checked':''}>${esc(m.name)} (#${m.userId})</label>`).join('')||'구성원이 없습니다.';}

$('#meetingForm').onsubmit=async e=>{e.preventDefault();if(!requireState('project'))return;const f=formData(e.target);const body={...f,agendas:f.agendas.split('\n').map(x=>x.trim()).filter(Boolean),expectedDurationMinutes:Number(f.expectedDurationMinutes),participantUserIds:$$('[name=participant]:checked').map(x=>Number(x.value))};try{setMeeting(await api(`/api/v1/projects/${state.project.id}/meetings`,{method:'POST',body:JSON.stringify(body)}));selectStep('interview');}catch{}};
async function loadMeetings(){if(!requireState('project'))return;try{const items=await api(`/api/v1/projects/${state.project.id}/meetings`);const el=$('#meetingList');if(!items.length){el.className='list empty';el.textContent='회의가 없습니다.';return}el.className='list';el.innerHTML=items.map(m=>`<div class="list-item"><div><b>#${m.id} ${esc(m.title)}</b><p>${m.meetingDate} ${m.startTime} · ${m.participantCount}명</p></div><button data-mid="${m.id}">선택</button></div>`).join('');$$('[data-mid]').forEach(b=>b.onclick=async()=>setMeeting(await api(`/api/v1/meetings/${b.dataset.mid}`)));}catch{}}
$('#loadMeetings').onclick=loadMeetings;

async function loadHistory(){
  const el=$('#historyList');
  try{
    const projects=await api('/api/v1/projects');
    const groups=await Promise.all(projects.map(async project=>({project,meetings:await api(`/api/v1/projects/${project.id}/meetings`)})));
    const rows=groups.flatMap(({project,meetings})=>meetings.map(meeting=>({project,meeting}))).sort((a,b)=>`${b.meeting.meetingDate} ${b.meeting.startTime}`.localeCompare(`${a.meeting.meetingDate} ${a.meeting.startTime}`));
    if(!rows.length){el.className='history-list empty';el.textContent='지난 회의가 없습니다.';return;}
    el.className='history-list';
    el.innerHTML=rows.map(({project,meeting})=>`<button class="history-row" data-history-mid="${meeting.id}"><span>${esc(meeting.title)}</span><span>${esc(project.name)}</span><span>${esc(meeting.purpose||'회의 목적 확인')}</span><span>${esc(meeting.meetingDate)}</span><i>›</i></button>`).join('');
    $$('[data-history-mid]').forEach(b=>b.onclick=async()=>{setMeeting(await api(`/api/v1/meetings/${b.dataset.historyMid}`));selectStep('analysis');await loadAnalysis();});
  }catch{}
}

function renderQuestions(data){state.questions=data.questions||[];const el=$('#questions');if(data.failureMessage)toast(data.failureMessage);if(!state.questions.length){el.className='empty';el.textContent=`상태: ${data.generationStatus} · 질문이 없습니다.`;$('#submitAnswers').hidden=true;return}el.className='';el.innerHTML=state.questions.map((q,i)=>`<label class="question">${i+1}. ${esc(q.content)}<textarea name="q-${q.id}" placeholder="솔직한 생각을 적어주세요" required></textarea></label>`).join('');$('#submitAnswers').hidden=false;}
async function questions(path=''){if(!requireState('meeting'))return;try{renderQuestions(await api(`/api/v1/meetings/${state.meeting.id}/interview/questions${path}`,path?{method:'POST'}:{}));}catch{}}
$('#loadQuestions').onclick=()=>questions();$('#retryQuestions').onclick=()=>questions('/generate');
$('#answerForm').onsubmit=async e=>{e.preventDefault();const answers=state.questions.map(q=>({questionId:q.id,content:new FormData(e.target).get(`q-${q.id}`)}));try{renderMyCard(await api(`/api/v1/meetings/${state.meeting.id}/interview/submissions`,{method:'POST',body:JSON.stringify({answers})}));}catch{}};
function renderMyCard(d){$('#myCard').innerHTML=`<div class="result"><h3>내 아이디어 카드 · ${d.cardGenerationStatus}</h3>${d.failureMessage?`<p>${esc(d.failureMessage)}</p>`:''}${d.ideaCard?ideaHtml(d.ideaCard):''}</div>`;}
function ideaHtml(c){return `<div class="idea"><b>${esc(c.coreOpinion)}</b><p><strong>이유</strong> ${esc(c.rationale)}</p><p><strong>우려</strong> ${esc(c.concern)}</p><p><strong>대안</strong> ${esc(c.alternative)}</p></div>`;}

$('#loadCards').onclick=async()=>{if(!requireState('meeting'))return;try{const cards=await api(`/api/v1/meetings/${state.meeting.id}/idea-cards`);const el=$('#cards');el.className=cards.length?'cards':'cards empty';el.innerHTML=cards.length?cards.map((c,i)=>`<div><small>ANONYMOUS ${String(i+1).padStart(2,'0')}</small>${ideaHtml(c)}</div>`).join(''):'등록된 카드가 없습니다.';}catch{}};
$('#refreshSummary').onclick=async()=>{if(!requireState('meeting'))return;try{renderSummary(await api(`/api/v1/meetings/${state.meeting.id}/idea-summary/refresh`,{method:'POST'}));}catch{}};
function renderSummary(s){$('#summary').innerHTML=`<div class="summary"><small>AI SUMMARY · VERSION ${s.version}</small><h4>공통 의견</h4><p>${esc(s.commonOpinions)}</p><h4>서로 다른 의견</h4><p>${esc(s.differingOpinions)}</p><h4>주요 우려</h4><p>${esc(s.keyConcerns)}</p><h4>논의할 부분</h4><p>${esc(s.discussionPoints)}</p></div>`;}

$('#recordForm').onsubmit=async e=>{e.preventDefault();if(!requireState('meeting'))return;try{await api(`/api/v1/meetings/${state.meeting.id}/record`,{method:'POST',body:JSON.stringify(formData(e.target))});toast('저장했습니다. AI 분석 결과를 불러옵니다.');await loadAnalysis();}catch{}};
$('#fileForm').onsubmit=async e=>{e.preventDefault();if(!requireState('meeting'))return;const data=new FormData(e.target);try{await api(`/api/v1/meetings/${state.meeting.id}/record/file`,{method:'POST',body:data});toast('파일을 업로드했습니다. AI 분석 결과를 불러옵니다.');await loadAnalysis();}catch{}};
async function loadAnalysis(path=''){if(!requireState('meeting'))return;try{renderAnalysis(await api(`/api/v1/meetings/${state.meeting.id}/result${path}`,path?{method:'POST'}:{}));}catch{}}
$('#loadAnalysis').onclick=()=>loadAnalysis();$('#retryAnalysis').onclick=()=>loadAnalysis('/generate');
function renderAnalysis(a){$('#analysisResult').innerHTML=`<div class="result"><h3>분석 상태 · ${a.status}</h3>${a.failureMessage?`<p>${esc(a.failureMessage)}</p>`:''}<h4>회의 목적</h4><p>${esc(a.meetingPurpose)}</p><h4>주요 논의</h4><p>${esc(a.keyDiscussions)}</p><h4>결정된 내용</h4><p>${esc(a.decisions)}</p><h4>추가 확인</h4><p>${esc(a.followUpChecks)}</p><h4>모호한 표현</h4>${(a.ambiguities||[]).map(x=>`<div class="ambiguity"><b>“${esc(x.expression)}”</b><p>${esc(x.reason)}</p></div>`).join('')||'<p>탐지된 표현이 없습니다.</p>'}</div>`;}

document.querySelector('[name=meetingDate]').value=new Date(Date.now()+86400000).toISOString().slice(0,10);
fetch('/actuator/health').then(r=>r.ok?r.json():Promise.reject()).then(()=>{$('#serverStatus').textContent='● 서버 정상';$('#serverStatus').classList.add('ok')}).catch(()=>$('#serverStatus').textContent='● 서버 연결 실패');
renderAuth();
