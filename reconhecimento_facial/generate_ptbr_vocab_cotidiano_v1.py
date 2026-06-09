import json, datetime, re

created_at = datetime.date(2026,5,28).isoformat()

# Curated everyday PT-BR vocabulary for ASR/voice intent bootstrapping.
# Note: this is an assistant-curated list (not a dump from a specific copyrighted dictionary).

core_function = [
    # articles / pronouns / prepositions / conjunctions / determiners
    'a','o','as','os','um','uma','uns','umas','ao','aos','à','às','no','na','nos','nas','do','da','dos','das','dum','duma','num','numa','nuns','numas',
    'de','em','por','para','pra','pro','com','sem','sobre','entre','até','desde','contra','perante','após','antes','durante','perto','longe','dentro','fora','atrás','acima','abaixo',
    'e','ou','mas','porque','pois','então','logo','se','quando','enquanto','como','onde','quem','qual','quais','quanto','quantos','quanta','quantas','que','isso','isto','aquilo','aqui','aí','ali','lá','cá',
    'eu','tu','você','vc','ele','ela','nós','a gente','vocês','eles','elas','me','mim','te','ti','se','si','lhe','lhes','nos','vos',
    'meu','minha','meus','minhas','teu','tua','teus','tuas','seu','sua','seus','suas','nosso','nossa','nossos','nossas',
    'este','esta','estes','estas','esse','essa','esses','essas','aquele','aquela','aqueles','aquelas','cada','todo','toda','todos','todas','algum','alguma','alguns','algumas','nenhum','nenhuma','nenhuns','nenhumas','muito','muita','muitos','muitas','pouco','pouca','poucos','poucas',
    'mais','menos','bem','mal','já','ainda','sempre','nunca','talvez','também','só','apenas','quase','mesmo','mesma','mesmos','mesmas',
]

numbers = [
    'zero','um','dois','três','quatro','cinco','seis','sete','oito','nove','dez','onze','doze','treze','catorze','quatorze','quinze','dezesseis','dezessete','dezoito','dezenove',
    'vinte','trinta','quarenta','cinquenta','sessenta','setenta','oitenta','noventa','cem','cento',
    'duzentos','trezentos','quatrocentos','quinhentos','seiscentos','setecentos','oitocentos','novecentos',
    'mil','milhão','milhões','bilhão','bilhões','primeiro','segunda','terceiro','quarto','quinto','sexto','sétimo','oitavo','nono','décimo'
]

time_calendar = [
    'hoje','ontem','amanhã','agora','depois','antes','cedo','tarde','noite','madrugada','manhã',
    'hora','horas','minuto','minutos','segundo','segundos','dia','dias','semana','semanas','mês','meses','ano','anos',
    'segunda','terça','quarta','quinta','sexta','sábado','domingo',
    'janeiro','fevereiro','março','abril','maio','junho','julho','agosto','setembro','outubro','novembro','dezembro'
]

common_verbs_base = [
    # infinitives
    'ser','estar','ter','haver','fazer','ir','vir','ver','dar','dizer','falar','ouvir','pensar','saber','poder','querer','precisar','gostar','amar','odiar','achar',
    'colocar','tirar','deixar','pegar','levar','trazer','chegar','sair','entrar','voltar','começar','terminar','parar','continuar',
    'ajudar','usar','criar','abrir','fechar','mandar','pedir','perguntar','responder','explicar','entender','lembrar','esquecer',
    'trabalhar','estudar','aprender','ensinar','jogar','dormir','acordar','comer','beber','cozinhar','limpar','lavar','comprar','vender','pagar',
    'ligar','desligar','baixar','instalar','atualizar','enviar','receber','salvar','carregar','buscar','procurar','clicar','digitar','gravar','apagar'
]

common_verb_forms = [
    # very common conjugated/colloquial forms (ASR-friendly)
    'é','são','sou','era','tava','tô','está','estão','estou','estive','ficar','fica','ficou','vai','vou','vamos','vão','ia','iria','vem','venho','veio','vi','viu',
    'tem','tenho','tinha','teve','tive','há','havia',
    'faz','faço','fiz','fez','faria','fazendo',
    'pode','posso','podia','pude','poderia',
    'quer','quero','queria','quis','vai querer',
    'precisa','preciso','precisava',
    'gosta','gosto','gostei','gostaria',
    'dá','dou','dei','deu',
    'diz','digo','disse','falou','falo','falei','fala',
    'manda','manda aí','manda pra mim','pede','peço','pedi',
    'cheguei','chega','chegou','saí','sai','sair','entra','entrou','voltei','volta','voltou'
]

# Common nouns: home, people, food, transport, work, daily life
nouns_people = [
    'pessoa','gente','homem','mulher','criança','menino','menina','adulto','idoso','amigo','amiga','namorado','namorada','marido','esposa',
    'pai','mãe','filho','filha','irmão','irmã','avô','avó','tio','tia','primo','prima','vizinho','vizinha',
    'professor','professora','aluno','aluna','médico','médica','enfermeiro','enfermeira','atendente','cliente','chefe','gerente','colega',
]

nouns_home = [
    'casa','apartamento','quarto','sala','cozinha','banheiro','porta','janela','chave','cama','mesa','cadeira','sofá','armário','geladeira','fogão','micro-ondas','chuveiro',
    'luz','água','energia','tomada','fio','internet','wifi','roteador','telefone','celular','carregador','fones','computador','notebook','teclado','mouse','tela',
]

nouns_city_transport = [
    'rua','avenida','bairro','centro','cidade','parque','praça','loja','mercado','supermercado','farmácia','padaria','banco','shopping','escola','faculdade','hospital','posto',
    'ônibus','metrô','trem','carro','moto','bicicleta','uber','taxi','viagem','passagem','bilhete','estação','ponto','trânsito','mapa','endereço',
]

nouns_food = [
    'comida','bebida','água','café','chá','suco','refrigerante','cerveja','vinho','pão','arroz','feijão','macarrão','carne','frango','peixe','ovo','salada','legume','fruta',
    'banana','maçã','laranja','limão','uva','manga','morango','batata','tomate','cebola','alho','queijo','leite','iogurte','manteiga','açúcar','sal','pimenta','óleo','azeite',
    'pizza','hambúrguer','sanduíche','bolo','doce','chocolate','sorvete'
]

nouns_health = [
    'saúde','doença','dor','febre','tosse','gripe','remédio','receita','consulta','exame','vacina','alergia','pressão','sangue','cabeça','garganta','estômago','costas','perna','braço','olho','dente',
]

nouns_work_study = [
    'trabalho','emprego','estágio','projeto','tarefa','prazo','reunião','agenda','equipe','time','cliente','documento','arquivo','pasta','planilha','apresentação','email','mensagem','chat','chamada','link','site','sistema','app','aplicativo',
    'código','teste','erro','bug','versão','build','deploy','servidor','rede','senha','login','conta','perfil','configuração','atualização','backup','download','upload',
    'aula','prova','nota','curso','certificado','treino','estudo','matéria','trabalho acadêmico'
]

nouns_money_shopping = [
    'dinheiro','preço','valor','custo','cartão','crédito','débito','pix','boleto','nota fiscal','desconto','promoção','compra','venda','pedido','entrega','frete','troca','garantia',
]

adjectives_adverbs = [
    'bom','boa','melhor','pior','ótimo','ótima','ruim','legal','chato','difícil','fácil','rápido','devagar','caro','barato','grande','pequeno','novo','nova','velho','velha',
    'certo','errado','pronto','pronta','cheio','cheia','vazio','vazia','longe','perto','quente','frio','cedo','tarde','claro','escuro',
    'agora','hoje','sempre','nunca','talvez','bem','mal','muito','pouco'
]

interjections = [
    'oi','olá','e aí','beleza','blz','bom dia','boa tarde','boa noite','valeu','obrigado','obrigada','por favor','desculpa','foi mal','com licença','tchau','até logo','até mais',
    'sim','não','ok','okay','certo','beleza então','bora','vamos lá'
]

# Flatten word-like entries: split multiword interjections into phrases later
phrases = [
    # daily phrases useful for voice
    'bom dia','boa tarde','boa noite','com licença','por favor','muito obrigado','muito obrigada','desculpa','foi mal','tudo bem','tá tudo bem','como você está','como cê tá',
    'pode repetir','repete por favor','fala de novo','mais devagar','mais rápido','não entendi','entendi','me ajuda','preciso de ajuda',
    'quanto custa','onde fica','como chegar','me chama','me liga','manda mensagem','me envia o link','abre o aplicativo','fecha o aplicativo',
    'liga o wifi','desliga o wifi','aumenta o volume','abaixa o volume','tira o som','coloca no silencioso','ativa o bluetooth','desativa o bluetooth',
    'quero pedir comida','quero um café','quero água','quero ir embora','cheguei em casa','estou indo','já estou chegando',
    'marca uma reunião','envia um email','responde a mensagem','salva o arquivo','faz o download','faz o upload',
]

# Build a category map
categories = {
    'core_function': core_function,
    'numbers': numbers,
    'time_calendar': time_calendar,
    'verbs_infinitive': common_verbs_base,
    'verbs_common_forms': common_verb_forms,
    'nouns_people': nouns_people,
    'nouns_home': nouns_home,
    'nouns_city_transport': nouns_city_transport,
    'nouns_food': nouns_food,
    'nouns_health': nouns_health,
    'nouns_work_study': nouns_work_study,
    'nouns_money_shopping': nouns_money_shopping,
    'adjectives_adverbs': adjectives_adverbs,
}

# Extract words from categories, excluding items that are clearly phrases (contain space)
words = []
for lst in categories.values():
    for w in lst:
        if isinstance(w,str) and ' ' not in w:
            words.append(w)

# Add a few high-value colloquial tokens
extra = [
    'tá','cê','pra','pro','cadê','uai','oxe','oxente','eita','vixi','poxa','tipo','mano','cara','galera','grana','trampo','rolê','mó','véi','véi','véio','véia',
    'whatsapp','zap','instagram','google','youtube','spotify','pix','boleto'
]
words.extend(extra)

# Normalize: keep accents; lower-case; strip
def norm(s):
    s = s.strip().lower()
    # collapse multiple spaces (for phrases)
    s = re.sub(r"\s+"," ",s)
    return s

words = [norm(w) for w in words]
phrases = [norm(p) for p in phrases]

# Deduplicate while preserving insertion order
seen=set(); ordered_words=[]
for w in words:
    if w and w not in seen:
        seen.add(w); ordered_words.append(w)

seen=set(); ordered_phrases=[]
for p in phrases:
    if p and p not in seen:
        seen.add(p); ordered_phrases.append(p)

# Some simple alias mapping (common abbreviations)
aliases = {
    'você':['vc','cê'],
    'pra':['para'],
    'pro':['para o'],
    'tá':['está'],
    'tô':['estou'],
    'blz':['beleza'],
    'zap':['whatsapp'],
}

payload = {
    'language':'pt-BR',
    'created_at': created_at,
    'schema':'vocab_v1',
    'notes':'Lista curada de vocabulário e frases do cotidiano (pt-BR) para bootstrap de ASR/NLU. Não é um dicionário completo; é um conjunto prático e extensível.',
    'counts': {
        'words': len(ordered_words),
        'phrases': len(ordered_phrases),
        'categories': {k: len([norm(x) for x in v if isinstance(x,str)]) for k,v in categories.items()}
    },
    'aliases': aliases,
    'categories': {k: [norm(x) for x in v] for k,v in categories.items()},
    'words': ordered_words,
    'phrases': ordered_phrases,
}

out_path = 'ptbr_vocab_cotidiano_v1.json'
with open(out_path,'w',encoding='utf-8') as f:
    json.dump(payload,f,ensure_ascii=False,indent=2)

print(json.dumps({'out_path': out_path, 'counts': payload['counts']}, ensure_ascii=False, indent=2))
