import {Client} from '/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/lib/node_modules/@picoxr/pico-cli/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StdioClientTransport} from '/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/lib/node_modules/@picoxr/pico-cli/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js';
import http from 'node:http';
const client=new Client({name:'spacebend-asset-authoring',version:'1.0.0'},{capabilities:{}});
await client.connect(new StdioClientTransport({command:'/Users/chanzungsan/.local/lib/nodejs/node-v22.23.2-darwin-arm64/bin/pico-cli',args:['editor','bootstrap'],stderr:'inherit'}));
http.createServer(async(req,res)=>{try{let body='';for await(const chunk of req)body+=chunk; const result=req.url==='/tools'?await client.listTools():await client.callTool(JSON.parse(body),undefined,{timeout:300000});res.setHeader('Content-Type','application/json');res.end(JSON.stringify(result));}catch(e){res.statusCode=500;res.end(JSON.stringify({error:String(e)}));}}).listen(8766,'127.0.0.1',()=>console.log('Editor MCP bridge ready on loopback 8766'));
