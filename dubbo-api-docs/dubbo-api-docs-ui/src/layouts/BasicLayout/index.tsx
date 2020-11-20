/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React, { useState } from 'react';
import { Shell, Input, Button } from '@alifd/next';
import PageNav from './components/PageNav';
import Footer from './components/Footer';
import { request } from 'ice';
import { LANGUAGE_KEY, LANGUAGE_SWITCH } from '../../constants';
import { emit } from '../../emit.js'
import intl from 'react-intl-universal';

export default function BasicLayout(props: {
  children: React.ReactNode;
  pathname: string;
}) {
  const { children, pathname } = props;

  const [dubboIp, setDubboIp] = useState('127.0.0.1');

  const [dubboPort, setDubboPort] = useState('20881');

  const [menuData4Nav, setData4Nav] = useState(new Array());

  const locale = intl.get('basicLayout');

  async function loadMenus(){
    const response = await request({
      url: '/api/apiModuleList',
      method: 'get',
      params: {
        dubboIp: dubboIp,
        dubboPort: dubboPort
      },
    }).catch(error => {
      console.log(error);
    });
    let resultData = new Array();
    if(response && response != ''){
      const menuData = JSON.parse(response);
      menuData.sort((a,b) => {
        return a.moduleDocName > b.moduleDocName;
      });
      for(let i = 0; i < menuData.length; i++){
        const menu = menuData[i];
        menu.moduleApiList.sort((a,b) => {
          return a.apiName > b.apiName;
        });
        const menu2 = {
          name: menu.moduleDocName,
          path: '',
          icon: 'cascades',
          children: new Array(),
        };
        const menuItems = menu.moduleApiList;
        for(let j = 0; j < menuItems.length; j++){
          const menuItem = menuItems[j];
          const menuItem2 = {
            name: menuItem.apiDocName,
            path: `/apiForm?apiName=${menu.moduleClassName}.${menuItem.apiName}&dubboIp=${dubboIp}&dubboPort=${dubboPort}`,
          };
          menu2.children.push(menuItem2);
        }
        resultData.push(menu2);
      }
    }
    setData4Nav(resultData);
  }

  function switchLanguage() {
    let currLang = localStorage.getItem(LANGUAGE_KEY);
    let langChangeTo = currLang === 'zh-CN' ? 'en-US' : 'zh-CN';
    localStorage.setItem(LANGUAGE_KEY, langChangeTo);
    emit.emit(LANGUAGE_SWITCH, langChangeTo);
  }

  return (
    <Shell
      type="dark"
      style={{
        minHeight: '100vh',
      }}
    >
      <Shell.Branding style={{width: '100%'}}>
        <div style={{float: 'left', width: '200px'}}>Dubbo API Docs</div>
        <div style={{ float: 'right'}}>
          <span className='language-switch'
            onClick={ switchLanguage }
          >
            {locale.switchLocale}
          </span>
        </div>
        <div style={{float: 'right', marginLeft: '30px'}}>
          <span>{locale.dubboProviderIP}</span>
          <Input
            htmlType={'text'}
            style={{ marginLeft: 5, width: 200 }}
            placeholder={'127.0.0.1'}
            value={dubboIp}
            onChange={setDubboIp}
          />
          <span style={{ marginLeft: 10 }}>{locale.dubboProviderPort}</span>
          <Input
            htmlType={'number'}
            style={{ marginLeft: 5, width: 80 }}
            placeholder={'20880'}
            value={dubboPort}
            onChange={setDubboPort}
          />
          <Button
            type={'primary'}
            style={{ marginLeft: 10 }}
            onClick={ loadMenus }
          >
            {locale.loadApiList}
          </Button>
        </div>
      </Shell.Branding>

      <Shell.Navigation>
        <PageNav pathname={pathname} menuData={menuData4Nav} />
      </Shell.Navigation>

    <Shell.Content>{children}</Shell.Content>
      <Shell.Footer>
        <Footer />
      </Shell.Footer>
    </Shell>
  );
}
